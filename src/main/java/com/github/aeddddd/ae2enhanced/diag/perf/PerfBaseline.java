package com.github.aeddddd.ae2enhanced.diag.perf;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 性能基准线：持久化到 {@code <world>/ae2enhanced/perf/baseline.json}.
 *
 * <p>记录 2 分钟窗口的平均 tick 耗时、网格总 tick 耗时、Top10 机器类耗时，
 * 供 {@link PerfAlerts} 周期比对实现异常预警。手动重建：{@code /ae2e perf baseline set}。</p>
 */
public final class PerfBaseline {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double DEFAULT_ALERT_MULTIPLIER = 1.5;

    /** 基准线数据。 */
    public static final class Baseline {
        public long createdAt;
        public double avgTickMs;
        public double alertMultiplier = DEFAULT_ALERT_MULTIPLIER;
        public long gridTotalNanos;
        /** 机器类名 → 平均 tick 耗时（纳秒），Top10 */
        public Map<String, Long> machineTotals = new LinkedHashMap<>();
    }

    private static Baseline cached;
    private static File cachedFile;
    private static long cachedMtime = -1L;

    private PerfBaseline() {
    }

    @Nullable
    private static File baselineFile(MinecraftServer server) {
        WorldServer overworld = server.getWorld(0);
        if (overworld == null) {
            return null;
        }
        return new File(new File(overworld.getSaveHandler().getWorldDirectory(), "ae2enhanced/perf"),
                "baseline.json");
    }

    /** 读取基准线（带 mtime 缓存）；不存在或损坏返回 null。 */
    @Nullable
    public static synchronized Baseline load(MinecraftServer server) {
        File file = baselineFile(server);
        if (file == null || !file.exists()) {
            cached = null;
            cachedFile = file;
            cachedMtime = -1L;
            return null;
        }
        if (cached != null && file.equals(cachedFile) && file.lastModified() == cachedMtime) {
            return cached;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            Baseline b = new Baseline();
            b.createdAt = json.get("createdAt").getAsLong();
            b.avgTickMs = json.get("avgTickMs").getAsDouble();
            b.alertMultiplier = json.has("alertMultiplier")
                    ? json.get("alertMultiplier").getAsDouble() : DEFAULT_ALERT_MULTIPLIER;
            b.gridTotalNanos = json.get("gridTotalNanos").getAsLong();
            if (json.has("machineTotals")) {
                for (Map.Entry<String, com.google.gson.JsonElement> e
                        : json.getAsJsonObject("machineTotals").entrySet()) {
                    b.machineTotals.put(e.getKey(), e.getValue().getAsLong());
                }
            }
            cached = b;
            cachedFile = file;
            cachedMtime = file.lastModified();
            return b;
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E-Perf] 基准线读取失败: {}", e.toString());
            return null;
        }
    }

    /** 从当前运行状态捕获新基准线；数据不足（启动未满 1 分钟）返回 null。 */
    @Nullable
    public static Baseline capture(MinecraftServer server) {
        TpsTracker.Stats stats = TpsTracker.stats(1200);
        if (stats == null) {
            return null;
        }
        Baseline b = new Baseline();
        b.createdAt = System.currentTimeMillis();
        b.avgTickMs = stats.avgMs;
        Baseline existing = load(server);
        if (existing != null) {
            b.alertMultiplier = existing.alertMultiplier; // 保留已配置的预警倍数
        }
        PerfAnalyzer.ScanResult scan = PerfAnalyzer.scan(-1.0);
        long gridTotal = 0;
        for (PerfAnalyzer.GridStat gs : scan.grids) {
            gridTotal += gs.totalAvgNanos;
        }
        b.gridTotalNanos = gridTotal;
        int rank = 0;
        for (PerfAnalyzer.MachineStat ms : scan.machines) {
            if (++rank > 10) break;
            b.machineTotals.put(ms.className, ms.totalAvgNanos);
        }
        return b;
    }

    public static synchronized boolean save(MinecraftServer server, Baseline b) {
        File file = baselineFile(server);
        if (file == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            return false;
        }
        JsonObject json = new JsonObject();
        json.addProperty("createdAt", b.createdAt);
        json.addProperty("avgTickMs", b.avgTickMs);
        json.addProperty("alertMultiplier", b.alertMultiplier);
        json.addProperty("gridTotalNanos", b.gridTotalNanos);
        JsonObject machines = new JsonObject();
        for (Map.Entry<String, Long> e : b.machineTotals.entrySet()) {
            machines.addProperty(e.getKey(), e.getValue());
        }
        json.add("machineTotals", machines);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
            cached = b;
            cachedFile = file;
            cachedMtime = file.lastModified();
            return true;
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E-Perf] 基准线写入失败: {}", e.toString());
            return false;
        }
    }

    public static synchronized boolean clear(MinecraftServer server) {
        File file = baselineFile(server);
        cached = null;
        cachedFile = file;
        cachedMtime = -1L;
        return file != null && file.exists() && file.delete();
    }
}
