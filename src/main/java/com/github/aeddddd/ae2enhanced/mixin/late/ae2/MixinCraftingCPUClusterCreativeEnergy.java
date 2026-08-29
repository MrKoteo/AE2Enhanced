package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.networking.TileCreativeEnergyCell;
import com.github.aeddddd.ae2enhanced.mixin.bridge.ICreativeEnergyAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 创造能源元件在场时移除发配链路的能量检查.
 *
 * <p>原生 executeCrafting 每次发配前按 1× 输入总数预扣能量
 * （{@code eg.extractAEPower(sum, MODULATE, ...)}，不足则跳过本次发配）。
 * 创造能源元件（{@link TileCreativeEnergyCell}）提供无限能量，此时该检查
 * 只会成为大订单的发配门槛，无实际意义。本 mixin 在网络中存在创造能源元件时
 * 令该扣费直接返回全额（不实际扣取）。</p>
 *
 * <p>检测结果按集群缓存 100 tick（网格成员变化频率低；创造元件放置后最多
 * 延迟 5 秒生效）。批量发配的 (batch-1)× 补扣由
 * {@code MixinCraftingCPUClusterAggregate} 通过 {@link ICreativeEnergyAccess} 同步跳过。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterCreativeEnergy implements ICreativeEnergyAccess {

    /** 检测结果缓存时长（tick）. */
    @Unique
    private static final long AE2E_CREATIVE_CHECK_INTERVAL = 100L;

    @Unique
    private boolean ae2e$creativeEnergy;

    @Unique
    private long ae2e$creativeEnergyCheckTick = Long.MIN_VALUE;

    @Shadow
    private World getWorld() {
        return null;
    }

    @Shadow
    private IGrid getGrid() {
        return null;
    }

    @Override
    public boolean ae2e$hasCreativeEnergy(IEnergyGrid eg) {
        World world = this.getWorld();
        long now = world != null ? world.getTotalWorldTime() : Long.MIN_VALUE;
        if (now != Long.MIN_VALUE && now - this.ae2e$creativeEnergyCheckTick < AE2E_CREATIVE_CHECK_INTERVAL) {
            return this.ae2e$creativeEnergy;
        }
        this.ae2e$creativeEnergyCheckTick = now;
        boolean found = false;
        try {
            IGrid grid = this.getGrid();
            if (grid != null) {
                found = !grid.getMachines(TileCreativeEnergyCell.class).isEmpty();
            }
        } catch (Throwable ignored) {
        }
        this.ae2e$creativeEnergy = found;
        return found;
    }

    /**
     * 创造能源在场时跳过 executeCrafting 的每次发配能量预扣.
     */
    @WrapOperation(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/energy/IEnergyGrid;extractAEPower(DLappeng/api/config/Actionable;Lappeng/api/config/PowerMultiplier;)D"),
            require = 0)
    private double ae2e$skipEnergyChargeWhenCreative(IEnergyGrid eg, double amt, Actionable mode,
            PowerMultiplier pm, Operation<Double> original) {
        if (ae2e$hasCreativeEnergy(eg)) {
            return amt;
        }
        return original.call(eg, amt, mode, pm);
    }
}
