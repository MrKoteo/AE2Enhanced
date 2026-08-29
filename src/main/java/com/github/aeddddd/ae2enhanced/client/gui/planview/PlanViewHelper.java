package com.github.aeddddd.ae2enhanced.client.gui.planview;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanClientCache;
import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanInfo;

/**
 * 合成计划/CPU 状态界面的列表视图助手: 过滤(搜索) + 混合排序 + 模式持久化.
 * <p>visual 列表在每次 postUpdate 后由本类从 storage/active/pending/missing
 * 三类列表整体重建: 合并同名项总量, 应用搜索过滤, 再按当前模式完整排序.</p>
 */
public final class PlanViewHelper {

    /** 标题栏截断宽度(像素), 为右上角搜索框留出空间. */
    private static final int TITLE_MAX_WIDTH = 140;

    private PlanViewHelper() {
    }

    // ==================== 模式存取(持久化到客户端配置) ====================

    public static PlanSortMode confirmMode() {
        return parse(AE2EnhancedConfig.craftingDisplay.confirmSortMode, PlanSortMode.CONFIRM_CYCLE);
    }

    public static PlanSortMode cpuMode() {
        return parse(AE2EnhancedConfig.craftingDisplay.cpuSortMode, PlanSortMode.CPU_CYCLE);
    }

    public static PlanSortMode cycleConfirmMode(boolean backwards) {
        PlanSortMode next = cycle(PlanSortMode.CONFIRM_CYCLE, confirmMode(), backwards);
        AE2EnhancedConfig.craftingDisplay.confirmSortMode = next.name();
        ConfigManager.sync(AE2Enhanced.MOD_ID, Config.Type.INSTANCE);
        return next;
    }

    public static PlanSortMode cycleCpuMode(boolean backwards) {
        PlanSortMode next = cycle(PlanSortMode.CPU_CYCLE, cpuMode(), backwards);
        AE2EnhancedConfig.craftingDisplay.cpuSortMode = next.name();
        ConfigManager.sync(AE2Enhanced.MOD_ID, Config.Type.INSTANCE);
        return next;
    }

    private static PlanSortMode parse(String raw, PlanSortMode[] cycle) {
        if (raw != null) {
            try {
                PlanSortMode mode = PlanSortMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                for (PlanSortMode allowed : cycle) {
                    if (allowed == mode) {
                        return mode;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // 非法值回落默认
            }
        }
        return cycle[0];
    }

    private static PlanSortMode cycle(PlanSortMode[] cycle, PlanSortMode current, boolean backwards) {
        int idx = 0;
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == current) {
                idx = i;
                break;
            }
        }
        int next = (idx + (backwards ? -1 : 1)) % cycle.length;
        if (next < 0) {
            next += cycle.length;
        }
        return cycle[next];
    }

    // ==================== 视图重建(过滤 + 排序) ====================

    /** 合成确认界面: 从 storage/pending/missing 重建 visual 并排序. */
    public static void refreshConfirm(List<IAEItemStack> visual, IItemList<IAEItemStack> storage,
            IItemList<IAEItemStack> pending, IItemList<IAEItemStack> missing,
            String filter, PlanSortMode mode) {
        collect(visual, storage, pending, missing, filter);
        visual.sort(confirmComparator(mode, visual, missing, pending));
    }

    /** CPU 状态界面: 从 storage/active/pending 重建 visual 并排序. */
    public static void refreshCpu(List<IAEItemStack> visual, IItemList<IAEItemStack> storage,
            IItemList<IAEItemStack> active, IItemList<IAEItemStack> pending,
            String filter, PlanSortMode mode, CraftingDurationTracker durations) {
        collect(visual, storage, active, pending, filter);
        visual.sort(cpuComparator(mode, active, pending, durations));
    }

    /** 三列表并集重建 visual: 总量 > 0 且匹配搜索过滤的项, 代表项 stackSize = 三表总量. */
    private static void collect(List<IAEItemStack> visual, IItemList<IAEItemStack> a,
            IItemList<IAEItemStack> b, IItemList<IAEItemStack> c, String filter) {
        visual.clear();
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        Set<String> seen = new HashSet<>();
        collectFrom(visual, seen, a, a, b, c, f);
        collectFrom(visual, seen, b, a, b, c, f);
        collectFrom(visual, seen, c, a, b, c, f);
    }

    private static void collectFrom(List<IAEItemStack> visual, Set<String> seen, IItemList<IAEItemStack> list,
            IItemList<IAEItemStack> a, IItemList<IAEItemStack> b, IItemList<IAEItemStack> c, String filter) {
        for (IAEItemStack s : list) {
            long total = countOf(a, s) + countOf(b, s) + countOf(c, s);
            if (total <= 0) {
                continue;
            }
            if (!seen.add(keyOf(s))) {
                continue;
            }
            if (!filter.isEmpty() && !displayName(s).toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            IAEItemStack rep = s.copy();
            rep.setStackSize(total);
            visual.add(rep);
        }
    }

    // ==================== 混合排序比较器 ====================

    private static Comparator<IAEItemStack> confirmComparator(PlanSortMode mode, List<IAEItemStack> visual,
            IItemList<IAEItemStack> missing, IItemList<IAEItemStack> pending) {
        NameCache names = new NameCache();
        switch (mode) {
            case TOTAL_DESC:
                return Comparator.comparingLong(IAEItemStack::getStackSize).reversed().thenComparing(names);
            case TO_CRAFT_DESC:
                return Comparator.<IAEItemStack> comparingLong(s -> countOf(pending, s)).reversed()
                        .thenComparing(Comparator.comparingLong(IAEItemStack::getStackSize).reversed())
                        .thenComparing(names);
            case SPECIAL_FIRST: {
                // 预计算: 0=特殊计划(自增殖/循环链), 1=有调用次数的普通样板, 2=其余
                Map<String, Integer> groups = new HashMap<>();
                Map<String, Long> metrics = new HashMap<>();
                for (IAEItemStack s : visual) {
                    int group = 2;
                    long metric = s.getStackSize();
                    SpecialPlanInfo info = SpecialPlanClientCache.infoFor(s.asItemStackRepresentation());
                    if (info != null) {
                        SpecialPlanInfo.Entry entry = info.entryFor(s);
                        if (entry != null) {
                            group = 0;
                            metric = entry.kind == SpecialPlanInfo.KIND_SELF_DUP ? entry.totalCrafts : entry.rounds;
                        } else {
                            long calls = info.callCountOf(s);
                            if (calls > 0) {
                                group = 1;
                                metric = calls;
                            }
                        }
                    }
                    groups.put(keyOf(s), group);
                    metrics.put(keyOf(s), metric);
                }
                return Comparator.<IAEItemStack> comparingInt(s -> groups.get(keyOf(s)))
                        .thenComparing(Comparator.<IAEItemStack> comparingLong(s -> metrics.get(keyOf(s))).reversed())
                        .thenComparing(names);
            }
            case MISSING_FIRST:
            default:
                return Comparator.<IAEItemStack> comparingInt(s -> countOf(missing, s) > 0 ? 0 : 1)
                        .thenComparing(Comparator.comparingLong(IAEItemStack::getStackSize).reversed())
                        .thenComparing(names);
        }
    }

    private static Comparator<IAEItemStack> cpuComparator(PlanSortMode mode,
            IItemList<IAEItemStack> active, IItemList<IAEItemStack> pending,
            CraftingDurationTracker durations) {
        NameCache names = new NameCache();
        switch (mode) {
            case TOTAL_DESC:
                return Comparator.comparingLong(IAEItemStack::getStackSize).reversed().thenComparing(names);
            case DURATION_DESC:
                return Comparator.<IAEItemStack> comparingInt(s -> countOf(active, s) > 0 ? 0 : 1)
                        .thenComparing(Comparator.<IAEItemStack> comparingLong(durations::elapsedMs).reversed())
                        .thenComparing(Comparator.<IAEItemStack> comparingLong(s -> countOf(active, s)
                                + countOf(pending, s)).reversed())
                        .thenComparing(names);
            case ACTIVE_FIRST:
            default:
                return Comparator.<IAEItemStack> comparingInt(s -> countOf(active, s) > 0 ? 0 : 1)
                        .thenComparing(Comparator.<IAEItemStack> comparingLong(s -> countOf(active, s)
                                + countOf(pending, s)).reversed())
                        .thenComparing(names);
        }
    }

    // ==================== 工具 ====================

    /** 标题栏截断: 超出宽度时以 ... 结尾, 为右上角搜索框留位. */
    public static String truncateTitle(String title) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        if (fr.getStringWidth(title) <= TITLE_MAX_WIDTH) {
            return title;
        }
        return fr.trimStringToWidth(title, TITLE_MAX_WIDTH - fr.getStringWidth("...")) + "...";
    }

    /** 物品身份键(注册名 + meta + NBT), 用于跨列表去重与计时跟踪. */
    public static String keyOf(IAEItemStack s) {
        ItemStack is = s.asItemStackRepresentation();
        if (is.isEmpty()) {
            return "empty";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Item.REGISTRY.getNameForObject(is.getItem()));
        sb.append('@').append(is.getMetadata());
        NBTTagCompound tag = is.getTagCompound();
        if (tag != null) {
            sb.append('#').append(tag);
        }
        return sb.toString();
    }

    private static long countOf(IItemList<IAEItemStack> list, IAEItemStack s) {
        IAEItemStack found = list.findPrecise(s);
        return found == null ? 0L : found.getStackSize();
    }

    private static String displayName(IAEItemStack s) {
        ItemStack is = s.asItemStackRepresentation();
        return is.isEmpty() ? "" : is.getDisplayName();
    }

    /** 显示名缓存比较器(排序兜底, 避免比较时重复构造 ItemStack). */
    private static final class NameCache implements Comparator<IAEItemStack> {
        private final Map<String, String> cache = new HashMap<>();

        @Override
        public int compare(IAEItemStack a, IAEItemStack b) {
            return name(a).compareToIgnoreCase(name(b));
        }

        private String name(IAEItemStack s) {
            String key = keyOf(s);
            String name = cache.get(key);
            if (name == null) {
                name = displayName(s);
                cache.put(key, name);
            }
            return name;
        }
    }
}
