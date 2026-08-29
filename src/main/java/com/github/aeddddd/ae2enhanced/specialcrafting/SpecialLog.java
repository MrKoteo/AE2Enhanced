package com.github.aeddddd.ae2enhanced.specialcrafting;

import com.github.aeddddd.ae2enhanced.diag.DiagLog;
import com.github.aeddddd.ae2enhanced.diag.DiagSwitch;

/**
 * 特殊配方/DAG 计划引擎诊断日志门控.
 *
 * <p>已迁移为 {@link DiagLog} 的转发壳（开关 {@link DiagSwitch#SPECIAL_CRAFTING}），
 * 保留原 API 使既有调用点零改动。默认关闭，运行期用
 * {@code /ae2e debug specialcrafting on|off} 控制。</p>
 */
public final class SpecialLog {

    private SpecialLog() {
    }

    public static boolean isEnabled() {
        return DiagLog.isEnabled(DiagSwitch.SPECIAL_CRAFTING);
    }

    public static void info(String message, Object... args) {
        DiagLog.info(DiagSwitch.SPECIAL_CRAFTING, message, args);
    }

    public static void warn(String message, Object... args) {
        DiagLog.warn(DiagSwitch.SPECIAL_CRAFTING, message, args);
    }
}
