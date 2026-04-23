package com.github.aeddddd.ae2enhanced.gui;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.container.ContainerAssemblyPattern;
import com.github.aeddddd.ae2enhanced.network.PacketPatternPage;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiAssemblyPattern extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            AE2Enhanced.MOD_ID, "textures/gui/assembly_pattern.png");

    private static final int PREV_BUTTON_ID = 1;
    private static final int NEXT_BUTTON_ID = 2;

    private final TileAssemblyController tile;
    private int page;
    private GuiButtonTech prevButton;
    private GuiButtonTech nextButton;

    public GuiAssemblyPattern(InventoryPlayer playerInv, TileAssemblyController tile, int page) {
        super(new ContainerAssemblyPattern(playerInv, tile, page));
        this.xSize = 350;
        this.ySize = 234;
        this.tile = tile;
        this.page = page;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();

        int cx = (this.width - this.xSize) / 2;
        int cy = (this.height - this.ySize) / 2;

        this.prevButton = new GuiButtonTech(PREV_BUTTON_ID, cx + 20, cy + 200, 40, 16,
                I18n.format("gui.ae2enhanced.pattern.prev"));
        this.nextButton = new GuiButtonTech(NEXT_BUTTON_ID, cx + 290, cy + 200, 40, 16,
                I18n.format("gui.ae2enhanced.pattern.next"));

        this.buttonList.add(this.prevButton);
        this.buttonList.add(this.nextButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        int maxPage = tile.getPatternPages() - 1;
        if (this.prevButton != null) {
            this.prevButton.enabled = page > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.enabled = page < maxPage;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonStates();

        // 如果当前页因容量变化超出可用范围，自动跳转到最后一页
        int maxPage = tile.getPatternPages() - 1;
        if (page > maxPage && maxPage >= 0) {
            mc.player.openGui(AE2Enhanced.instance, GuiHandler.encodePatternId(maxPage),
                mc.world, tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
            return;
        }

        if (tile.isFormed()) {
            // 结构被解散时关闭当前GUI（由服务端控制主状态机）
        }
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
        int maxPage = tile.getPatternPages() - 1;
        if (button.id == PREV_BUTTON_ID) {
            int targetPage = Math.max(0, page - 1);
            AE2Enhanced.network.sendToServer(new PacketPatternPage(tile.getPos(), targetPage));
        } else if (button.id == NEXT_BUTTON_ID) {
            int targetPage = Math.min(maxPage, page + 1);
            AE2Enhanced.network.sendToServer(new PacketPatternPage(tile.getPos(), targetPage));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("gui.ae2enhanced.pattern.title");
        this.fontRenderer.drawString(title,
                (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 8, 0x404040);

        String pageStr = I18n.format("gui.ae2enhanced.pattern.page",
                page + 1, tile.getPatternPages());
        this.fontRenderer.drawString(pageStr,
                (this.xSize - this.fontRenderer.getStringWidth(pageStr)) / 2, 200, 0x404040);
    }
}
