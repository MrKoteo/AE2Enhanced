package com.github.aeddddd.ae2enhanced.diag;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.diag.check.CheckResult;
import com.github.aeddddd.ae2enhanced.diag.check.DiagChecks;
import com.github.aeddddd.ae2enhanced.diag.check.SystemCheck;
import com.github.aeddddd.ae2enhanced.diag.metrics.Counter;
import com.github.aeddddd.ae2enhanced.diag.metrics.Gauge;
import com.github.aeddddd.ae2enhanced.diag.metrics.MetricsRegistry;
import com.github.aeddddd.ae2enhanced.diag.metrics.Timer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 诊断报告生成器：汇总环境信息 + 全部系统检查 + 指标快照 + 近期异常事件，
 * 落盘到 {@code logs/ae2enhanced/diag-<时间戳>.txt}.
 */
public final class DiagReport {

    /** 报告关注的条件 mod 列表 */
    private static final String[] RELEVANT_MODS = {
            "appliedenergistics2", "mekanism", "mekeng", "thaumcraft",
            "thaumicenergistics", "botania", "astralsorcery", "projecte", "jei"
    };

    private DiagReport() {
    }

    /**
     * 生成报告并写盘.
     *
     * @return 写入的文件；写入失败返回 null
     */
    public static File generate(MinecraftServer server) {
        Map<SystemCheck, List<CheckResult>> results = DiagChecks.runAll(server);

        File dir = new File("logs/ae2enhanced");
        if (!dir.exists() && !dir.mkdirs()) {
            AE2Enhanced.LOGGER.error("创建诊断报告目录失败: {}", dir.getAbsolutePath());
            return null;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(dir, "diag-" + timestamp + ".txt");

        try (PrintWriter w = new PrintWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), StandardCharsets.UTF_8))) {
            writeHeader(w);
            writeEnvironment(w);
            writeCheckResults(w, results);
            writeTps(w);
            writeMetrics(w);
            writeEvents(w);
            w.flush();
            return file;
        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("写入诊断报告失败: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private static void writeHeader(PrintWriter w) {
        w.println("===============================================================");
        w.println(" AE2Enhanced 诊断报告");
        w.println(" 生成时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        w.println("===============================================================");
        w.println();
    }

    private static void writeEnvironment(PrintWriter w) {
        w.println("## 环境信息");
        w.println("AE2Enhanced 版本: " + AE2Enhanced.VERSION);
        w.println("Minecraft: 1.12.2 / Forge: " + net.minecraftforge.common.ForgeVersion.getVersion());
        w.println("相关 mod:");
        for (String modId : RELEVANT_MODS) {
            boolean loaded = Loader.isModLoaded(modId);
            w.println("  - " + modId + ": " + (loaded ? "已安装" : "未安装"));
        }
        w.println("诊断开关:");
        for (String name : DiagSwitch.names()) {
            w.println("  - " + name + ": " + (DiagSwitch.isEnabled(name) ? "ON" : "OFF")
                    + (DiagSwitch.hasOverride(name) ? " (运行期覆盖)" : " (内置默认)"));
        }
        w.println();
    }

    private static void writeCheckResults(PrintWriter w, Map<SystemCheck, List<CheckResult>> results) {
        w.println("## 系统检查");
        int totalOk = 0;
        int totalWarn = 0;
        int totalError = 0;
        for (Map.Entry<SystemCheck, List<CheckResult>> entry : results.entrySet()) {
            w.println("### " + entry.getKey().displayName() + " (" + entry.getKey().name() + ")");
            for (CheckResult r : entry.getValue()) {
                w.println("  " + r);
                switch (r.level) {
                    case OK: totalOk++; break;
                    case WARN: totalWarn++; break;
                    case ERROR: totalError++; break;
                }
            }
            w.println();
        }
        w.println("检查汇总: OK=" + totalOk + " WARN=" + totalWarn + " ERROR=" + totalError);
        w.println();
    }

    private static void writeTps(PrintWriter w) {
        w.println("## 服务器 TPS/tick 耗时");
        writeTpsWindow(w, "5s", 100);
        writeTpsWindow(w, "1min", 1200);
        writeTpsWindow(w, "2min", 2400);
        w.println();
    }

    private static void writeTpsWindow(PrintWriter w, String label, int window) {
        com.github.aeddddd.ae2enhanced.diag.perf.TpsTracker.Stats stats =
                com.github.aeddddd.ae2enhanced.diag.perf.TpsTracker.stats(window);
        w.println("  " + label + ": " + (stats == null ? "暂无数据" : stats.toString()));
    }

    private static void writeMetrics(PrintWriter w) {
        w.println("## 指标快照");
        boolean any = false;
        for (Counter c : MetricsRegistry.counters()) {
            w.println("  [counter] " + c.name() + " = " + c.get());
            any = true;
        }
        for (Gauge g : MetricsRegistry.gauges()) {
            w.println("  [gauge] " + g.name() + " = "
                    + String.format(Locale.ROOT, "%.2f", g.get()));
            any = true;
        }
        for (Timer t : MetricsRegistry.timers()) {
            w.println("  [timer] " + t.name() + " " + t.snapshot());
            any = true;
        }
        if (!any) {
            w.println("  (无已注册指标)");
        }
        w.println();
    }

    private static void writeEvents(PrintWriter w) {
        w.println("## 近期异常事件(最多 50 条,新→旧)");
        List<DiagEvents.Event> events = DiagEvents.latest(50);
        if (events.isEmpty()) {
            w.println("  (无事件)");
        } else {
            for (DiagEvents.Event e : events) {
                w.println("  " + e);
            }
        }
        w.println();
    }
}
