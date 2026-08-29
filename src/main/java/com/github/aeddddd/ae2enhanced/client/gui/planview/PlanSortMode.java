package com.github.aeddddd.ae2enhanced.client.gui.planview;

import java.util.Locale;

import net.minecraft.client.resources.I18n;

/**
 * 合成计划/CPU 状态界面的排序模式(混合排序: 分组优先 + 组内指标降序).
 * <p>每个界面有独立的切换循环, 当前模式持久化在客户端配置中.</p>
 */
public enum PlanSortMode {

    /** 缺料组置顶, 组内按总需求降序(合成确认界面默认). */
    MISSING_FIRST,
    /** 全部按总需求降序. */
    TOTAL_DESC,
    /** 全部按待合成数降序. */
    TO_CRAFT_DESC,
    /** 特殊计划(自增殖/循环链)置顶, 其次是有调用次数的普通样板, 其余按总需求降序. */
    SPECIAL_FIRST,
    /** 进行中(active > 0)置顶, 组内按 active+pending 降序(CPU 界面默认). */
    ACTIVE_FIRST,
    /** 进行中置顶, 组内按子项已持续时间降序. */
    DURATION_DESC;

    /** 合成确认界面的切换循环. */
    public static final PlanSortMode[] CONFIRM_CYCLE = { MISSING_FIRST, TOTAL_DESC, TO_CRAFT_DESC, SPECIAL_FIRST };
    /** CPU 状态界面的切换循环. */
    public static final PlanSortMode[] CPU_CYCLE = { ACTIVE_FIRST, DURATION_DESC, TOTAL_DESC };

    public String label() {
        return I18n.format("gui.ae2enhanced.plan_sort.mode." + name().toLowerCase(Locale.ROOT));
    }
}
