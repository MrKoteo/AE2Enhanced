package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.github.aeddddd.ae2enhanced.mixin.bridge.ICraftingCpuElapsed;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 CraftingCPUStatus 增加"当前任务已持续时间"字段并随 NBT 同步.
 * <p>服务端构造(ICraftingCPU, int)时从 {@link CraftingCPUCluster#getElapsedTime()} 捕获;
 * 客户端经 NBT 构造函数还原. CPU 列表每 20 tick 全量同步一次,
 * 客户端在两次同步间按本地时钟外推, 显示平滑递增.</p>
 */
@Mixin(value = CraftingCPUStatus.class, remap = false)
public abstract class MixinCraftingCPUStatus implements ICraftingCpuElapsed {

    @Unique
    private long ae2enhanced$elapsedTimeNanos;

    /** 服务端构造: 捕获集群已耗时(仅忙碌时有意义). */
    @Inject(method = "<init>(Lappeng/api/networking/crafting/ICraftingCPU;I)V", at = @At("RETURN"))
    private void ae2enhanced$captureElapsed(ICraftingCPU cluster, int serial, CallbackInfo ci) {
        if (cluster instanceof CraftingCPUCluster && cluster.isBusy()) {
            this.ae2enhanced$elapsedTimeNanos = ((CraftingCPUCluster) cluster).getElapsedTime();
        }
    }

    /** 客户端构造: 从 NBT 还原. */
    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    private void ae2enhanced$readElapsed(NBTTagCompound tag, CallbackInfo ci) {
        this.ae2enhanced$elapsedTimeNanos = tag.getLong("ae2e_elapsed");
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2enhanced$writeElapsed(NBTTagCompound tag, CallbackInfo ci) {
        if (this.ae2enhanced$elapsedTimeNanos > 0L) {
            tag.setLong("ae2e_elapsed", this.ae2enhanced$elapsedTimeNanos);
        }
    }

    @Override
    public long ae2enhanced$getElapsedTimeNanos() {
        return this.ae2enhanced$elapsedTimeNanos;
    }
}
