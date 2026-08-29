package com.github.aeddddd.ae2enhanced.diag.perf;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.diag.metrics.Counter;
import com.github.aeddddd.ae2enhanced.diag.metrics.Gauge;
import com.github.aeddddd.ae2enhanced.diag.metrics.MetricsRegistry;
import com.github.aeddddd.ae2enhanced.diag.metrics.Timer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 性能数据导出器：{@code /ae2e perf export} 将 TPS 窗口统计、网格/机器/慢节点扫描、
 * 指标注册表全量快照写入 {@code logs/ae2enhanced/perf-<时间戳>.json}，
 * 供外部工具（Excel/Grafana 等）做多维对比与趋势分析.
 */
public final class PerfExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PerfExporter() {
    }

    @Nullable
    public static File export(MinecraftServer server) {
        JsonObject root = new JsonObject();
        root.addProperty("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        root.addProperty("modVersion", AE2Enhanced.VERSION);
        root.add("tps", buildTpsSection());
        root.add("scan", buildScanSection());
        root.add("metrics", buildMetricsSection());

        File dir = new File("logs/ae2enhanced");
        if (!dir.exists() && !dir.mkdirs()) {
            AE2Enhanced.LOGGER.error("创建性能导出目录失败: {}", dir.getAbsolutePath());
            return null;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(dir, "perf-" + timestamp + ".json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            return file;
        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("写入性能导出失败: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private static JsonObject buildTpsSection() {
        JsonObject tps = new JsonObject();
        addTpsWindow(tps, "5s", 100);
        addTpsWindow(tps, "1min", 1200);
        addTpsWindow(tps, "2min", 2400);
        return tps;
    }

    private static void addTpsWindow(JsonObject tps, String label, int window) {
        TpsTracker.Stats stats = TpsTracker.stats(window);
        if (stats == null) {
            return;
        }
        JsonObject w = new JsonObject();
        w.addProperty("samples", stats.samples);
        w.addProperty("tps", stats.tps);
        w.addProperty("avgMs", stats.avgMs);
        w.addProperty("minMs", stats.minMs);
        w.addProperty("maxMs", stats.maxMs);
        w.addProperty("p95Ms", stats.p95Ms);
        tps.add(label, w);
    }

    private static JsonObject buildScanSection() {
        JsonObject section = new JsonObject();
        PerfAnalyzer.ScanResult scan = PerfAnalyzer.scan(-1.0);
        section.addProperty("scanMs", scan.scanMs());

        JsonArray grids = new JsonArray();
        int rank = 0;
        for (PerfAnalyzer.GridStat gs : scan.grids) {
            JsonObject g = new JsonObject();
            g.addProperty("rank", ++rank);
            g.addProperty("nodes", gs.nodes);
            g.addProperty("totalAvgNanos", gs.totalAvgNanos);
            g.addProperty("avgPowerUsage", gs.avgPowerUsage);
            g.addProperty("storedPower", gs.storedPower);
            g.addProperty("cpuBusy", gs.cpuBusy);
            g.addProperty("cpuTotal", gs.cpuTotal);
            g.addProperty("controllerState", String.valueOf(gs.controllerState));
            grids.add(g);
        }
        section.add("grids", grids);

        JsonArray machines = new JsonArray();
        for (PerfAnalyzer.MachineStat ms : scan.machines) {
            JsonObject m = new JsonObject();
            m.addProperty("className", ms.className);
            m.addProperty("nodes", ms.nodes);
            m.addProperty("totalAvgNanos", ms.totalAvgNanos);
            machines.add(m);
        }
        section.add("machines", machines);
        return section;
    }

    private static JsonObject buildMetricsSection() {
        JsonObject metrics = new JsonObject();
        JsonObject counters = new JsonObject();
        for (Counter c : MetricsRegistry.counters()) {
            counters.addProperty(c.name(), c.get());
        }
        metrics.add("counters", counters);

        JsonObject timers = new JsonObject();
        for (Timer t : MetricsRegistry.timers()) {
            Timer.Snapshot s = t.snapshot();
            JsonObject obj = new JsonObject();
            obj.addProperty("count", s.count);
            obj.addProperty("avgMs", s.avgMs);
            obj.addProperty("minMs", s.minMs);
            obj.addProperty("maxMs", s.maxMs);
            obj.addProperty("p50Ms", s.p50Ms);
            obj.addProperty("p95Ms", s.p95Ms);
            obj.addProperty("p99Ms", s.p99Ms);
            timers.add(t.name(), obj);
        }
        metrics.add("timers", timers);

        JsonObject gauges = new JsonObject();
        for (Gauge g : MetricsRegistry.gauges()) {
            gauges.addProperty(g.name(), g.get());
        }
        metrics.add("gauges", gauges);
        return metrics;
    }
}
