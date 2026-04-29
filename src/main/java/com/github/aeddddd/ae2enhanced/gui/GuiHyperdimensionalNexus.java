package com.github.aeddddd.ae2enhanced.gui;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.tile.TileHyperdimensionalController;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 超维度仓储中枢信息面板。
 * 纯展示 GUI，无物品槽位，不与玩家背包交互。
 */
public class GuiHyperdimensionalNexus extends GuiScreen {

    private final TileHyperdimensionalController tile;
    private int xSize = 176;
    private int ySize = 140;
    private int guiLeft;
    private int guiTop;

    public GuiHyperdimensionalNexus(TileHyperdimensionalController tile) {
        this.tile = tile;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // 绘制面板背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xCC000000);
        drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFF2C2C2C);
        drawRect(guiLeft + 4, guiTop + 16, guiLeft + xSize - 4, guiTop + ySize - 4, 0xFF1A1A1A);

        // 标题
        String title = "§b超维度仓储中枢";
        this.fontRenderer.drawString(title, guiLeft + 8, guiTop + 6, 0x00FFFF);

        if (tile == null) {
            this.fontRenderer.drawString("§cTile 不可用", guiLeft + 8, guiTop + 20, 0xFF5555);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int y = guiTop + 22;
        int lineHeight = 12;

        // 结构状态
        String formedStr = tile.isFormed() ? "§a已组装" : "§c未组装";
        this.fontRenderer.drawString("结构状态: " + formedStr, guiLeft + 8, y, 0xFFFFFF);
        y += lineHeight;

        // 网络状态
        String networkStr = tile.isNetworkActive() ? "§a在线" : "§c离线";
        this.fontRenderer.drawString("网络状态: " + networkStr, guiLeft + 8, y, 0xFFFFFF);
        y += lineHeight;

        // 能源状态
        String powerStr = tile.isNetworkPowered() ? "§a供能正常" : "§e未供能";
        this.fontRenderer.drawString("能源状态: " + powerStr, guiLeft + 8, y, 0xFFFFFF);
        y += lineHeight;

        // Nexus ID
        if (tile.getNexusId() != null) {
            String id = tile.getNexusId().toString().substring(0, 8);
            this.fontRenderer.drawString("Nexus ID: §7" + id, guiLeft + 8, y, 0xFFFFFF);
            y += lineHeight;
        }

        // 存储统计
        if (tile.getItemAdapter() != null) {
            int types = tile.getItemAdapter().getStorageMap().size();
            BigInteger totalItems = BigInteger.ZERO;
            for (BigInteger count : tile.getItemAdapter().getStorageMap().values()) {
                totalItems = totalItems.add(count);
            }
            this.fontRenderer.drawString("存储种类: §e" + types, guiLeft + 8, y, 0xFFFFFF);
            y += lineHeight;
            this.fontRenderer.drawString("总物品数: §e" + formatBigNumber(totalItems), guiLeft + 8, y, 0xFFFFFF);
        } else {
            this.fontRenderer.drawString("存储核心: §7未初始化", guiLeft + 8, y, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String formatBigNumber(BigInteger num) {
        if (num.compareTo(BigInteger.valueOf(1_000_000_000_000L)) >= 0) {
            return num.toString(); // 超过万亿直接显示完整数字
        } else if (num.compareTo(BigInteger.valueOf(1_000_000_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000_000_000L)) + " B";
        } else if (num.compareTo(BigInteger.valueOf(1_000_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000_000L)) + " M";
        } else if (num.compareTo(BigInteger.valueOf(1_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000L)) + " K";
        }
        return num.toString();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            this.mc.displayGuiScreen(null);
        }
        super.keyTyped(typedChar, keyCode);
    }
}
