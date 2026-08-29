package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.client.gui.AEBaseGui;
import com.github.aeddddd.ae2enhanced.client.gui.planview.IPlanViewHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AEBaseGui 层的计划视图事件转发: mouseClicked.
 * <p>GuiCraftConfirm/GuiCraftingCPU 均未覆写 func_73864_a, 事件必经 AEBaseGui,
 * 在此统一转发给实现了 {@link IPlanViewHost} 的 GUI(搜索框焦点控制).</p>
 */
@Mixin(value = AEBaseGui.class, remap = false)
public abstract class MixinAEBaseGuiPlanHooks {

    @Inject(method = "func_73864_a", at = @At("HEAD"))
    private void ae2enhanced$dispatchPlanMouseClicked(int xCoord, int yCoord, int btn, CallbackInfo ci) {
        if ((Object) this instanceof IPlanViewHost) {
            ((IPlanViewHost) (Object) this).ae2enhanced$planMouseClicked(xCoord, yCoord, btn);
        }
    }
}
