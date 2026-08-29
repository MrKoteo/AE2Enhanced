package com.github.aeddddd.ae2enhanced.diag.perf;

import com.github.aeddddd.ae2enhanced.diag.DiagSwitch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Locale;

/**
 * 服务器 TPS / tick 耗时采集器.
 *
 * <p>挂在 {@link TickEvent.ServerTickEvent} END 相位，每 tick 记录一次
 * nanoTime 差值（即上一个 tick 周期总耗时），开销可忽略。
 * 环形缓冲保留最近 2 分钟（2400 tick）的原始数据，支持任意窗口统计。</p>
 *
 * <p>总开关：{@code /ae2e debug perf off} 停止采集（已采集数据保留）。</p>
 */
public final class TpsTracker {

    private static final int RING_CAPACITY = 2400; // 2 分钟 @20 TPS
    private static final long[] RING = new long[RING_CAPACITY];
    private static int pos = 0;
    private static int size = 0;
    private static long lastTickNanos = -1L;
    private static long tickCount = 0L;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.nanoTime();
        long prev = lastTickNanos;
        lastTickNanos = now;
        if (prev < 0 || !DiagSwitch.isEnabled(DiagSwitch.PERF)) {
            return;
        }
        long duration = now - prev;
        synchronized (RING) {
            RING[pos] = duration;
            pos = (pos + 1) % RING_CAPACITY;
            if (size < RING_CAPACITY) {
                size++;
            }
        }
        // 每分钟驱动一次基准线比对预警（需先 /ae2e perf baseline set）
        if (++tickCount % PerfAlerts.CHECK_INTERVAL_TICKS == 0) {
            PerfAlerts.check(FMLCommonHandler.instance().getMinecraftServerInstance());
        }
    }

    /** 最近 {@code window} 个 tick 的统计；无数据返回 null。 */
    public static Stats stats(int window) {
        long[] copy;
        synchronized (RING) {
            int n = Math.min(Math.min(window, size), RING_CAPACITY);
            if (n == 0) {
                return null;
            }
            copy = new long[n];
            // 从最新往前取 n 个
            for (int i = 0; i < n; i++) {
                int idx = pos - 1 - i;
                if (idx < 0) idx += RING_CAPACITY;
                copy[i] = RING[idx];
            }
        }
        Arrays.sort(copy);
        long sum = 0;
        for (long v : copy) sum += v;
        double avgMs = sum / (double) copy.length / 1_000_000.0;
        double p95Ms = copy[(int) Math.min(copy.length - 1, Math.max(0, Math.ceil(0.95 * copy.length) - 1))] / 1_000_000.0;
        double minMs = copy[0] / 1_000_000.0;
        double maxMs = copy[copy.length - 1] / 1_000_000.0;
        double tps = avgMs <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / avgMs);
        return new Stats(copy.length, avgMs, minMs, maxMs, p95Ms, tps);
    }

    /** 一个窗口（tick 数）的统计快照。 */
    public static final class Stats {
        public final int samples;
        public final double avgMs;
        public final double minMs;
        public final double maxMs;
        public final double p95Ms;
        public final double tps;

        Stats(int samples, double avgMs, double minMs, double maxMs, double p95Ms, double tps) {
            this.samples = samples;
            this.avgMs = avgMs;
            this.minMs = minMs;
            this.maxMs = maxMs;
            this.p95Ms = p95Ms;
            this.tps = tps;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "TPS=%.2f avg=%.2fms min=%.2fms max=%.2fms p95=%.2fms (n=%d)",
                    tps, avgMs, minMs, maxMs, p95Ms, samples);
        }
    }
}
