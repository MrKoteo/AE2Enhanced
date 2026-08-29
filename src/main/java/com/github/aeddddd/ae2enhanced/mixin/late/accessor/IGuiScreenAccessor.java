package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * GuiScreen.buttonList 访问器, 供计划视图 mixin 向 GUI 添加排序切换按钮.
 * <p>目标为 Minecraft 原生类, 使用默认 remap=true.</p>
 */
@Mixin(GuiScreen.class)
public interface IGuiScreenAccessor {

    @Accessor("buttonList")
    List<GuiButton> ae2enhanced$getButtonList();
}
