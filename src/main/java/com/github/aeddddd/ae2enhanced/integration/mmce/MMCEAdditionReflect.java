package com.github.aeddddd.ae2enhanced.integration.mmce;

/**
 * MMCE-addition（modid: mmceaddition）类的反射隔离访问.
 *
 * <p>本类常量池仅含字符串字面量，mmceaddition 未安装时所有判定返回 false。
 * 仅被 mmce 条件 mixin 引用，不得被无条件加载的类引用。</p>
 */
public final class MMCEAdditionReflect {

    /** mmceaddition ME 样板总成（ICraftingProvider, Long 缓冲）. */
    private static final Class<?> PATTERN_ASSEMBLY =
            find("com.github.aeddddd.mmceaddition.tile.TileMEPatternAssembly");

    /** mmceaddition 输入总成仓（纯 IItemHandler/IFluidHandler capability, Long 缓冲）. */
    private static final Class<?> INPUT_ASSEMBLY =
            find("com.github.aeddddd.mmceaddition.tile.TileInputAssembly");

    private MMCEAdditionReflect() {
    }

    private static Class<?> find(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isPatternAssembly(Object obj) {
        return PATTERN_ASSEMBLY != null && PATTERN_ASSEMBLY.isInstance(obj);
    }

    /** mmceaddition 是否可用（输入总成类存在即视为可用）. */
    public static boolean isAvailable() {
        return INPUT_ASSEMBLY != null;
    }

    public static boolean isInputAssembly(Object obj) {
        return INPUT_ASSEMBLY != null && INPUT_ASSEMBLY.isInstance(obj);
    }
}
