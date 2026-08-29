package com.github.aeddddd.ae2enhanced.diag.metrics;

import java.util.function.DoubleSupplier;

/**
 * 瞬时值指标：读取时从数据源拉取（如网格储能、通道占用率）.
 */
public final class Gauge {

    private final String name;
    private final DoubleSupplier supplier;

    Gauge(String name, DoubleSupplier supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    public String name() {
        return name;
    }

    public double get() {
        return supplier.getAsDouble();
    }
}
