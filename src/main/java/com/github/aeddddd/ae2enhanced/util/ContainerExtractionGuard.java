package com.github.aeddddd.ae2enhanced.util;

import java.lang.reflect.Method;

/**
 * Mekanism CEu v10 TileEntityContainerBlock#isContainerExtractionGuarded 反射访问.
 *
 * <p>编译期 classpath 同时存在新旧两个 Mekanism jar,共享类
 * TileEntityContainerBlock 被旧 jar（字母序靠前）遮蔽,CE 独有方法无法直接链接,
 * 故按项目反射隔离约定访问。本类仅被 mekceuv10 条件配置中的 mixin 引用,
 * 不会出现在无条件加载类的常量池中。
 * 不得放入 mixin 包（Mixin 禁止直接引用 mixin 包内的类）。</p>
 */
public final class ContainerExtractionGuard {

    private static final Method GUARD_METHOD;

    static {
        Method m = null;
        try {
            m = Class.forName("mekanism.common.tile.prefab.TileEntityContainerBlock")
                    .getMethod("isContainerExtractionGuarded", Object.class);
        } catch (Throwable ignored) {
            // CE 不存在时不会被加载,防御性兜底
        }
        GUARD_METHOD = m;
    }

    private ContainerExtractionGuard() {
    }

    /** 反射失败时回退为"未守卫"（等同该检查引入前的旧行为）. */
    public static boolean isGuarded(Object tileEntity, Object slot) {
        if (GUARD_METHOD == null) {
            return false;
        }
        try {
            return (Boolean) GUARD_METHOD.invoke(tileEntity, slot);
        } catch (Throwable t) {
            return false;
        }
    }
}
