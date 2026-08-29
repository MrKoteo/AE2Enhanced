package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.container.implementations.CraftingCPUStatus;
import com.github.aeddddd.ae2enhanced.client.gui.planview.CraftingDurationTracker;
import com.github.aeddddd.ae2enhanced.mixin.bridge.ICraftingCpuElapsed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CPU 选择列表(GuiCraftingStatus 左侧面板)增强:
 * 每个忙碌的 CPU 条目左下角显示当前任务已持续时间.
 * <p>数据来自 MixinCraftingCPUStatus 扩展的 NBT 同步(每 20 tick 全量一次),
 * 两次同步间按本地时钟外推, 显示平滑递增.</p>
 */
@Mixin(value = GuiCraftingStatus.class, remap = false)
public abstract class MixinGuiCraftingStatus {

    @Shadow
    private ContainerCraftingStatus status;

    @Shadow
    private GuiScrollbar cpuScrollbar;

    /** serial -> [同步基准(纳秒), 接收时刻(毫秒)]; 同步值变化时重置基准. */
    @Unique
    private final Map<Integer, long[]> ae2enhanced$elapsedBase = new HashMap<>();

    @Inject(method = "drawFG", at = @At("TAIL"), require = 0)
    private void ae2enhanced$drawCpuElapsed(int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        try {
            if (this.status == null || this.cpuScrollbar == null) {
                return;
            }
            List<CraftingCPUStatus> cpus = this.status.getCPUs();
            if (cpus.isEmpty()) {
                this.ae2enhanced$elapsedBase.clear();
                return;
            }
            int firstCpu = this.cpuScrollbar.getCurrentScroll();
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            long now = System.currentTimeMillis();
            for (int i = firstCpu; i < firstCpu + 6 && i < cpus.size(); i++) {
                CraftingCPUStatus cpu = cpus.get(i);
                if (cpu == null) {
                    continue;
                }
                long elapsedMs = ae2enhanced$elapsedMs(cpu, now);
                if (elapsedMs <= 0) {
                    continue;
                }
                String text = CraftingDurationTracker.format(elapsedMs);
                int x = -85;
                int y = 19 + (i - firstCpu) * 23;
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.5, 0.5, 0.5);
                // CPU 条目左下角(与名称/存储文本水平错开), 深灰小字
                fr.drawString(text, (x + 3) * 2, (y + 18) * 2, 0x404040);
                GlStateManager.popMatrix();
            }
            // 清理已不在列表中的 serial
            if (!this.ae2enhanced$elapsedBase.isEmpty()) {
                Iterator<Map.Entry<Integer, long[]>> it = this.ae2enhanced$elapsedBase.entrySet().iterator();
                while (it.hasNext()) {
                    int serial = it.next().getKey();
                    boolean present = false;
                    for (CraftingCPUStatus cpu : cpus) {
                        if (cpu != null && cpu.getSerial() == serial) {
                            present = true;
                            break;
                        }
                    }
                    if (!present) {
                        it.remove();
                    }
                }
            }
        } catch (Throwable ignored) {
            // 渲染增强失败静默
        }
    }

    /**
     * 该 CPU 当前任务的已持续时间(毫秒): 同步基准 + 本地外推.
     * 同步值变化(每次全量同步)时重置基准, 空闲(无合成目标)时返回 0 并清除.
     */
    @Unique
    private long ae2enhanced$elapsedMs(CraftingCPUStatus cpu, long now) {
        if (cpu.getCrafting() == null || !(cpu instanceof ICraftingCpuElapsed)) {
            this.ae2enhanced$elapsedBase.remove(cpu.getSerial());
            return 0L;
        }
        long syncedNanos = ((ICraftingCpuElapsed) cpu).ae2enhanced$getElapsedTimeNanos();
        if (syncedNanos <= 0L) {
            this.ae2enhanced$elapsedBase.remove(cpu.getSerial());
            return 0L;
        }
        long[] base = this.ae2enhanced$elapsedBase.get(cpu.getSerial());
        if (base == null || base[0] != syncedNanos) {
            base = new long[] { syncedNanos, now };
            this.ae2enhanced$elapsedBase.put(cpu.getSerial(), base);
        }
        return TimeUnit.NANOSECONDS.toMillis(base[0]) + Math.max(0L, now - base[1]);
    }
}
