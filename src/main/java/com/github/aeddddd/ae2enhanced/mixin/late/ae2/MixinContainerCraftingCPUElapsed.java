package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.container.implementations.ContainerCraftingCPU;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * CPU 状态界面标题时间改造: 用"已持续时间"替换"预计完成时间(ETA)".
 * <p>原生服务端每 tick 计算 ETA(elapsedTime / 已完成数 * 剩余数)并经 @GuiSync 同步,
 * 对波动大的合成毫无意义. 此 Mixin 将同步内容替换为
 * {@link CraftingCPUCluster#getElapsedTime()}(纳秒, NBT 持久化, 重启后延续),
 * 客户端标题格式化逻辑不变, 直接显示当前合成已持续时间.</p>
 */
@Mixin(value = ContainerCraftingCPU.class, remap = false)
public abstract class MixinContainerCraftingCPUElapsed {

    @Shadow
    private CraftingCPUCluster monitor;

    @WrapOperation(
        method = "func_75142_b",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/container/implementations/ContainerCraftingCPU;setEstimatedTime(J)V"
        ),
        require = 0
    )
    private void ae2enhanced$syncElapsedInsteadOfEta(ContainerCraftingCPU self, long eta,
            Operation<Void> original) {
        original.call(self, this.monitor != null ? this.monitor.getElapsedTime() : eta);
    }
}
