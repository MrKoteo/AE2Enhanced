package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import com.github.aeddddd.ae2enhanced.client.gui.planview.IPlanViewHost;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * GuiScreen 层的计划视图事件转发: keyTyped / updateScreen.
 * <p>目标是补获未自行覆写 keyTyped 的计划视图 GUI(如 GuiCraftingCPU)的键盘事件,
 * 供搜索框输入; 已覆写 keyTyped 的 GUI(如 GuiCraftConfirm)在自身 mixin 中先行消费,
 * 未消费的事件最终到达此处时接口实现会按焦点状态自行忽略.</p>
 * <p>目标为 Minecraft 原生类, 使用默认 remap=true.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenPlanHooks {

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ae2enhanced$dispatchPlanKeyTyped(char typedChar, int keyCode, CallbackInfo ci) throws IOException {
        if ((Object) this instanceof IPlanViewHost
                && ((IPlanViewHost) (Object) this).ae2enhanced$planKeyTyped(typedChar, keyCode)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    private void ae2enhanced$dispatchPlanUpdateScreen(CallbackInfo ci) {
        if ((Object) this instanceof IPlanViewHost) {
            ((IPlanViewHost) (Object) this).ae2enhanced$planUpdateScreen();
        }
    }
}
