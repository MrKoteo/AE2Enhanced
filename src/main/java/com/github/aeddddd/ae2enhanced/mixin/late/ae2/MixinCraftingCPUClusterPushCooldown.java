package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * CPU 推送失败冷却：消除"满载目标机器 + 大型合成"场景的每 tick 级联开销.
 *
 * <p>背景：{@code executeCrafting} 对每个未完成 task 每 tick 执行
 * "提取材料 → 逐 medium 试探 pushPattern → 全部失败后回灌材料"。
 * 目标机器满载时，每次 pushPattern 都会对所有朝向做 acceptsItems 全槽模拟扫描
 * （输入数 × 目标槽数 次 insertItem），并伴随 postChange 事件与库存回灌，
 * 形成每 tick 的扫描风暴。</p>
 *
 * <p>实现：per-medium 失败冷却表（{@code IdentityHashMap<ICraftingMedium, Long>}）。
 * pushPattern 返回 false 时记录当前 world time；冷却期内的 medium 在
 * {@code m.isBusy()} 判定处直接视为忙碌而跳过——由于该判定发生在材料提取之前，
 * 冷却期内的 stalled task 不再触发任何提取/扫描/回灌。</p>
 *
 * <p>冷却时长 5 tick 的语义安全性：接口消化 waitingToSend 队列最快也要
 * 一个接口 tick（TickRates.Interface min = 5 tick），因此 5 tick 内重试同一
 * medium 本来就不可能成功，冷却不会丢失任何本可成功的推送。</p>
 *
 * <p>两个 WrapOperation 与 MixinCraftingCPUClusterRemaining /
 * MixinCraftingCPUClusterVirtualBatch 对 pushPattern 的包装链式共存
 * （这些包装均原样透传 boolean 结果，嵌套位置不影响失败记录的准确性）。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterPushCooldown {

    @Unique
    private static final boolean AE2E_CRAZYAE_LOADED = Loader.isModLoaded("crazyae");

    /** 推送失败冷却时长（tick），对齐 TickRates.Interface 的最小 tick 间隔。 */
    @Unique
    private static final long AE2E_PUSH_FAIL_COOLDOWN_TICKS = 5L;

    /** 各 medium 最近一次 pushPattern 失败的 world time。表大小以网络接口数为上界，随 CPU 销毁回收。 */
    @Unique
    private final Map<ICraftingMedium, Long> ae2e$pushFailTicks = new IdentityHashMap<>();

    @Shadow
    private World getWorld() {
        return null;
    }

    /**
     * 冷却期内的 medium 直接视为忙碌。
     * isBusy 判定位于材料提取之前，跳过即可同时省去提取与全槽扫描。
     */
    @WrapOperation(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;isBusy()Z"),
            require = 0)
    private boolean ae2e$treatCooledMediumAsBusy(ICraftingMedium medium, Operation<Boolean> original) {
        if (!AE2E_CRAZYAE_LOADED) {
            Long failTick = this.ae2e$pushFailTicks.get(medium);
            if (failTick != null) {
                World world = this.getWorld();
                if (world != null && world.getTotalWorldTime() - failTick < AE2E_PUSH_FAIL_COOLDOWN_TICKS) {
                    return true;
                }
            }
        }
        return original.call(medium);
    }

    /**
     * 记录 pushPattern 失败时间，供 isBusy 包装查询。
     */
    @WrapOperation(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"))
    private boolean ae2e$recordPushFailure(ICraftingMedium medium, ICraftingPatternDetails details,
            InventoryCrafting table, Operation<Boolean> original) {
        boolean result = original.call(medium, details, table);
        if (!result && !AE2E_CRAZYAE_LOADED) {
            World world = this.getWorld();
            if (world != null) {
                this.ae2e$pushFailTicks.put(medium, world.getTotalWorldTime());
            }
        }
        return result;
    }
}
