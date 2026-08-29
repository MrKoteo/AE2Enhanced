package com.github.aeddddd.ae2enhanced.diag.perf;

import com.github.aeddddd.ae2enhanced.diag.DiagEvents;
import com.github.aeddddd.ae2enhanced.diag.DiagSwitch;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 性能异常预警：与基准线周期比对，超阈值向 OP 广播并写入异常事件流.
 *
 * <p>由 {@link TpsTracker} 每分钟驱动一次。比对项：</p>
 * <ul>
 *   <li>2 分钟窗口平均 tick 耗时 vs 基准线</li>
 *   <li>全网格总 tick 耗时 vs 基准线</li>
 *   <li>基准线 Top10 机器类的当前耗时 vs 基准值</li>
 * </ul>
 *
 * <p>预警只提示不干预；冷却 5 分钟防刷屏。需先 {@code /ae2e perf baseline set} 建立基准线。</p>
 */
public final class PerfAlerts {

    /** 比对间隔（1 分钟） */
    public static final int CHECK_INTERVAL_TICKS = 1200;
    private static final long ALERT_COOLDOWN_MS = 300_000L;

    private static long lastAlertMillis = 0L;

    private PerfAlerts() {
    }

    /** 每分钟由 TpsTracker 调用。 */
    public static void check(MinecraftServer server) {
        if (server == null || !DiagSwitch.isEnabled(DiagSwitch.PERF)) {
            return;
        }
        PerfBaseline.Baseline baseline = PerfBaseline.load(server);
        if (baseline == null) {
            return;
        }
        TpsTracker.Stats stats = TpsTracker.stats(1200);
        if (stats == null) {
            return;
        }
        double mult = baseline.alertMultiplier;

        List<String> alerts = new ArrayList<>();
        if (stats.avgMs > baseline.avgTickMs * mult) {
            alerts.add(String.format(Locale.ROOT,
                    "平均 tick 耗时 %.2fms 超过基准 %.2fms 的 %.1f 倍",
                    stats.avgMs, baseline.avgTickMs, mult));
        }

        PerfAnalyzer.ScanResult scan = PerfAnalyzer.scan(-1.0);
        long gridTotal = 0;
        for (PerfAnalyzer.GridStat gs : scan.grids) {
            gridTotal += gs.totalAvgNanos;
        }
        if (baseline.gridTotalNanos > 0 && gridTotal > baseline.gridTotalNanos * mult) {
            alerts.add(String.format(Locale.ROOT,
                    "网格总 tick 耗时 %s 超过基准 %s 的 %.1f 倍",
                    PerfAnalyzer.formatNanos(gridTotal),
                    PerfAnalyzer.formatNanos(baseline.gridTotalNanos), mult));
        }
        for (PerfAnalyzer.MachineStat ms : scan.machines) {
            Long base = baseline.machineTotals.get(ms.className);
            if (base != null && base > 0 && ms.totalAvgNanos > base * mult) {
                alerts.add(String.format(Locale.ROOT,
                        "机器类 %s 耗时 %s 超过基准 %s 的 %.1f 倍",
                        ms.className, PerfAnalyzer.formatNanos(ms.totalAvgNanos),
                        PerfAnalyzer.formatNanos(base), mult));
            }
        }

        if (alerts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAlertMillis < ALERT_COOLDOWN_MS) {
            return;
        }
        lastAlertMillis = now;
        for (String alert : alerts) {
            DiagEvents.warn("perf", alert);
        }
        broadcastToOps(server, alerts);
    }

    private static void broadcastToOps(MinecraftServer server, List<String> alerts) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (!player.canUseCommand(2, "ae2enhanced")) {
                continue;
            }
            player.sendMessage(new TextComponentString(TextFormatting.RED + "[AE2E-Perf] 性能异常预警:"));
            for (String alert : alerts) {
                player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "  " + alert));
            }
            player.sendMessage(new TextComponentString(TextFormatting.GRAY
                    + "  使用 /ae2e perf top / slow 定位来源,/ae2e perf baseline set 重建基准线"));
        }
    }
}
