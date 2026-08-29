package com.github.aeddddd.ae2enhanced.diag.check;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统检查注册表与执行器.
 */
public final class DiagChecks {

    private static final Map<String, SystemCheck> CHECKS = new LinkedHashMap<>();

    static {
        register(new StorageCheck());
        register(new ChannelCheck());
        register(new RecipeCheck());
        register(new GridCheck());
        register(new PersonalDimCheck());
    }

    private DiagChecks() {
    }

    private static void register(SystemCheck check) {
        CHECKS.put(check.name(), check);
    }

    public static List<SystemCheck> all() {
        return new ArrayList<>(CHECKS.values());
    }

    @Nullable
    public static SystemCheck byName(String name) {
        return CHECKS.get(name);
    }

    /** 执行单个检查并返回结果（含小节标题结果）。 */
    public static List<CheckResult> runOne(MinecraftServer server, SystemCheck check) {
        List<CheckResult> out = new ArrayList<>();
        check.run(server, out);
        return out;
    }

    /** 执行全部检查，按系统分组返回（保持注册顺序）。 */
    public static Map<SystemCheck, List<CheckResult>> runAll(MinecraftServer server) {
        Map<SystemCheck, List<CheckResult>> results = new LinkedHashMap<>();
        for (SystemCheck check : all()) {
            results.put(check, runOne(server, check));
        }
        return results;
    }

    /** 统计一组结果中某级别的数量。 */
    public static int countLevel(Iterable<CheckResult> results, CheckResult.Level level) {
        int n = 0;
        for (CheckResult r : results) {
            if (r.level == level) n++;
        }
        return n;
    }
}
