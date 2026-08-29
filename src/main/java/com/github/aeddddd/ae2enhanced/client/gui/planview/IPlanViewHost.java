package com.github.aeddddd.ae2enhanced.client.gui.planview;

/**
 * 计划视图(GuiCraftConfirm / GuiCraftingCPU)的搜索框事件接收接口.
 * <p>由 MixinAEBaseGuiPlanHooks(mouseClicked)与 MixinGuiScreenPlanHooks(keyTyped/updateScreen)
 * 转发事件; GUI 自身的 mixin 实现此接口处理搜索框交互.</p>
 */
public interface IPlanViewHost {

    /** 鼠标点击(由 AEBaseGui 钩子转发). */
    void ae2enhanced$planMouseClicked(int mouseX, int mouseY, int button);

    /** 按键(由 GuiScreen 钩子或 GUI 自身 mixin 转发). 返回 true 表示已消费. */
    boolean ae2enhanced$planKeyTyped(char typedChar, int keyCode);

    /** updateScreen 心跳(搜索框光标闪烁). */
    void ae2enhanced$planUpdateScreen();
}
