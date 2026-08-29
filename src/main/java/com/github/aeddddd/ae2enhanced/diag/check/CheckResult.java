package com.github.aeddddd.ae2enhanced.diag.check;

/**
 * 单条诊断检查结果.
 */
public final class CheckResult {

    public enum Level {
        OK, WARN, ERROR
    }

    public final Level level;
    public final String message;

    private CheckResult(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    public static CheckResult ok(String message) {
        return new CheckResult(Level.OK, message);
    }

    public static CheckResult warn(String message) {
        return new CheckResult(Level.WARN, message);
    }

    public static CheckResult error(String message) {
        return new CheckResult(Level.ERROR, message);
    }

    @Override
    public String toString() {
        return "[" + level + "] " + message;
    }
}
