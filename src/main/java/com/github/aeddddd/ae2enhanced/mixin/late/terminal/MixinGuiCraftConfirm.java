package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftConfirm;
import com.github.aeddddd.ae2enhanced.client.gui.planview.IPlanViewHost;
import com.github.aeddddd.ae2enhanced.client.gui.planview.PlanViewHelper;
import com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanClientCache;
import com.github.aeddddd.ae2enhanced.mixin.late.accessor.IGuiScreenAccessor;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * 合成确认界面(GuiCraftConfirm)显示增强.
 * <ul>
 * <li>混合排序: 缺料组置顶, 组内按指标降序; 排序模式可由玩家点击按钮切换并持久化
 * (缺料优先 / 按数量 / 按待合成 / 特殊计划优先), 见 {@link PlanViewHelper}.</li>
 * <li>搜索框: 标题栏右侧, 按物品显示名过滤列表.</li>
 * <li>特殊计划显示: 悬停 tooltip 追加自增殖/循环链结构信息与样板调用次数,
 * 格子数量区下方追加行内灰色小字(显示层始终启用, 不受功能开关影响).</li>
 * </ul>
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public abstract class MixinGuiCraftConfirm implements IPlanViewHost {

    @Shadow
    private List<IAEItemStack> visual;

    @Shadow
    private IItemList<IAEItemStack> missing;

    @Shadow
    private IItemList<IAEItemStack> storage;

    @Shadow
    private IItemList<IAEItemStack> pending;

    @Shadow
    private int tooltip;

    @Unique
    private GuiButton ae2enhanced$sortButton;

    @Unique
    private GuiTextField ae2enhanced$searchField;

    @Unique
    private String ae2enhanced$searchText = "";

    // ==================== 列表重建(排序 + 搜索过滤) ====================

    @Inject(method = "postUpdate", at = @At("TAIL"))
    private void ae2enhanced$onPostUpdate(List<IAEItemStack> list, byte ref, CallbackInfo ci) {
        ae2enhanced$refreshView();
    }

    @Unique
    private void ae2enhanced$refreshView() {
        PlanViewHelper.refreshConfirm(this.visual, this.storage, this.pending, this.missing,
                this.ae2enhanced$searchText, PlanViewHelper.confirmMode());
        // 原生 setScrollBar 已在重建前执行, 用过滤后的尺寸重设范围(5 行视图)
        ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar()
                .setRange(0, (this.visual.size() + 2) / 3 - 5, 1);
    }

    // ==================== 排序按钮 + 搜索框 ====================

    @Inject(method = "func_73866_w_", at = @At("TAIL"))
    private void ae2enhanced$onInitGui(CallbackInfo ci) {
        GuiContainer self = (GuiContainer) (Object) this;
        int guiLeft = self.getGuiLeft();
        int guiTop = self.getGuiTop();
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        // 底部按钮行空位(Cancel 与 Start 之间)
        this.ae2enhanced$sortButton = new GuiButton(3090, guiLeft + 60, guiTop + 181, 100, 20, "");
        ((IGuiScreenAccessor) self).ae2enhanced$getButtonList().add(this.ae2enhanced$sortButton);
        this.ae2enhanced$searchField = new GuiTextField(3091, fr, guiLeft + 154, guiTop + 4, 78, 12);
        this.ae2enhanced$searchField.setMaxStringLength(64);
        this.ae2enhanced$searchField.setText(this.ae2enhanced$searchText);
        ae2enhanced$updateSortButtonText();
    }

    @Inject(method = "func_146284_a", at = @At("HEAD"))
    private void ae2enhanced$onActionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == this.ae2enhanced$sortButton) {
            PlanViewHelper.cycleConfirmMode(GuiScreen.isShiftKeyDown());
            ae2enhanced$updateSortButtonText();
            ae2enhanced$refreshView();
        }
    }

    @Unique
    private void ae2enhanced$updateSortButtonText() {
        if (this.ae2enhanced$sortButton != null) {
            this.ae2enhanced$sortButton.displayString = I18n.format(
                    "gui.ae2enhanced.plan_sort.button", PlanViewHelper.confirmMode().label());
        }
    }

    /**
     * 搜索框按键: 本类目标 GUI 自行覆写了 keyTyped(原生 Enter 触发 Start),
     * 必须在自身 HEAD 先消费, 等不到 GuiScreen 钩子转发.
     */
    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void ae2enhanced$onKeyTyped(char character, int key, CallbackInfo ci) {
        if (ae2enhanced$planKeyTyped(character, key)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_73863_a", at = @At("TAIL"))
    private void ae2enhanced$onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ae2enhanced$updateSortButtonText();
        if (this.ae2enhanced$searchField != null) {
            this.ae2enhanced$searchField.drawTextBox();
        }
        GuiScreen self = (GuiScreen) (Object) this;
        if (this.ae2enhanced$sortButton != null && ae2enhanced$isHover(this.ae2enhanced$sortButton.x,
                this.ae2enhanced$sortButton.y, this.ae2enhanced$sortButton.width,
                this.ae2enhanced$sortButton.height, mouseX, mouseY)) {
            self.drawHoveringText(Collections.singletonList(
                    I18n.format("gui.ae2enhanced.plan_sort.hint")), mouseX, mouseY);
        } else if (this.ae2enhanced$searchField != null && this.ae2enhanced$searchText.isEmpty()
                && !this.ae2enhanced$searchField.isFocused()
                && ae2enhanced$isHover(this.ae2enhanced$searchField.x, this.ae2enhanced$searchField.y,
                        this.ae2enhanced$searchField.width, this.ae2enhanced$searchField.height, mouseX, mouseY)) {
            self.drawHoveringText(Collections.singletonList(
                    I18n.format("gui.ae2enhanced.plan_search.hint")), mouseX, mouseY);
        }
    }

    @Unique
    private boolean ae2enhanced$isHover(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // ==================== IPlanViewHost(搜索框事件, 由钩子 mixin 转发) ====================

    @Override
    public void ae2enhanced$planMouseClicked(int mouseX, int mouseY, int button) {
        if (this.ae2enhanced$searchField != null) {
            this.ae2enhanced$searchField.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean ae2enhanced$planKeyTyped(char typedChar, int keyCode) {
        if (this.ae2enhanced$searchField == null || !this.ae2enhanced$searchField.isFocused()) {
            return false;
        }
        if (keyCode == 1) {
            return false; // ESC 放行: 关闭界面
        }
        if (keyCode == 28 || keyCode == 156) {
            this.ae2enhanced$searchField.setFocused(false);
            return true;
        }
        if (this.ae2enhanced$searchField.textboxKeyTyped(typedChar, keyCode)) {
            this.ae2enhanced$searchText = this.ae2enhanced$searchField.getText();
            ae2enhanced$refreshView();
        }
        return true; // 聚焦时吞掉其余按键, 避免触发界面快捷键(含原生 Enter -> Start)
    }

    @Override
    public void ae2enhanced$planUpdateScreen() {
        if (this.ae2enhanced$searchField != null) {
            this.ae2enhanced$searchField.updateCursorCounter();
        }
    }

    // ==================== 标题截断(为搜索框留位) ====================

    @WrapOperation(
        method = "drawFG",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;func_78276_b(Ljava/lang/String;III)I",
            ordinal = 0
        ),
        require = 0
    )
    private int ae2enhanced$truncateTitle(FontRenderer fr, String title, int x, int y, int color,
            Operation<Integer> original) {
        return original.call(fr, PlanViewHelper.truncateTitle(title), x, y, color);
    }

    // ==================== Special Plan Tooltip ====================

    /**
     * 特殊计划显示:在合成确认界面的悬停 tooltip 末尾追加
     * 自增殖/循环链结构信息与样板调用次数(显示层始终启用,不受功能开关影响).
     */
    @WrapOperation(
        method = "drawFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"
        ),
        require = 0
    )
    private void ae2enhanced$wrapPlanTooltip(GuiCraftConfirm self, int x, int y, String message,
            Operation<Void> original) {
        original.call(self, x, y, ae2enhanced$appendSpecialPlanLines(message));
    }

    @Unique
    private String ae2enhanced$appendSpecialPlanLines(String message) {
        try {
            IAEItemStack hovered = ae2enhanced$hoveredStack();
            if (hovered == null) {
                return message;
            }
            SpecialPlanInfo info = SpecialPlanClientCache.infoFor(hovered.asItemStackRepresentation());
            if (info == null) {
                return message;
            }
            StringBuilder sb = new StringBuilder(message);
            SpecialPlanInfo.Entry entry = info.entryFor(hovered);
            if (entry != null) {
                for (String line : com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                        .tooltipLines(hovered, entry)) {
                    sb.append('\n').append(line);
                }
            } else {
                long calls = info.callCountOf(hovered);
                if (calls > 0) {
                    sb.append('\n').append(com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                            .normalDescriptionLine(calls));
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return message;
        }
    }

    @Unique
    private IAEItemStack ae2enhanced$hoveredStack() {
        if (this.tooltip < 0 || this.visual == null) {
            return null;
        }
        int viewStart = ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar().getCurrentScroll() * 3;
        int idx = viewStart + this.tooltip;
        if (idx < 0 || idx >= this.visual.size()) {
            return null;
        }
        return this.visual.get(idx);
    }

    /**
     * 行内描述（1.1.0 对齐）:每个可见单元格的数量区下方追加一行灰色小字——
     * 自增殖"调用 N 次"/循环链"约 R 轮发配"/普通样板"调用 N 次".
     * 坐标方案与原生数量行一致(0.5 缩放、续接 downY 流).缓存为空时零影响.
     */
    @Inject(method = "drawFG", at = @At("TAIL"), require = 0)
    private void ae2enhanced$drawInlineDescriptions(int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        try {
            if (this.visual == null || this.visual.isEmpty()) {
                return;
            }
            int viewStart = ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar().getCurrentScroll() * 3;
            int viewEnd = viewStart + 15; // 3 列 × 5 行
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int x = 0;
            int y = 0;
            for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); z++) {
                IAEItemStack refStack = this.visual.get(z);
                if (refStack != null) {
                    String desc = ae2enhanced$inlineDesc(refStack);
                    if (desc != null) {
                        ae2enhanced$drawCellLine(fr, desc, x, y, z);
                    }
                }
                if (++x > 2) {
                    ++y;
                    x = 0;
                }
            }
        } catch (Throwable ignored) {
            // 渲染增强失败静默
        }
    }

    @Unique
    private String ae2enhanced$inlineDesc(IAEItemStack refStack) {
        SpecialPlanInfo info = SpecialPlanClientCache.infoFor(refStack.asItemStackRepresentation());
        if (info == null) {
            return null;
        }
        SpecialPlanInfo.Entry entry = info.entryFor(refStack);
        if (entry != null) {
            return com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                    .descriptionLine(entry);
        }
        long calls = info.callCountOf(refStack);
        if (calls > 0) {
            return com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                    .normalDescriptionLine(calls);
        }
        return null;
    }

    /**
     * 按原生数量行的坐标方案绘制一行(0.5 缩放、居中、续接 downY 流末尾).
     */
    @Unique
    private void ae2enhanced$drawCellLine(FontRenderer fr, String str, int x, int y, int z) {
        IAEItemStack refStack = this.visual.get(z);
        int lines = 0;
        if (this.storage != null) {
            IAEItemStack stored = this.storage.findPrecise(refStack);
            if (stored != null && stored.getStackSize() > 0) {
                lines++;
            }
        }
        if (this.missing != null) {
            IAEItemStack missingStack = this.missing.findPrecise(refStack);
            if (missingStack != null && missingStack.getStackSize() > 0) {
                lines++;
            }
        }
        if (this.pending != null) {
            IAEItemStack pendingStack = this.pending.findPrecise(refStack);
            if (pendingStack != null && pendingStack.getStackSize() > 0) {
                lines++;
            }
        }
        int negY = (lines - 1) * 5 / 2;
        int downY = lines * 5;
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.scale(0.5, 0.5, 0.5);
        int w = 4 + fr.getStringWidth(str);
        fr.drawString(str, (int) (((double) (x * 68 + 9 + 67 - 19) - (double) w * 0.5) * 2.0),
                (y * 23 + 22 + 6 - negY + downY) * 2, 0x404040);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
