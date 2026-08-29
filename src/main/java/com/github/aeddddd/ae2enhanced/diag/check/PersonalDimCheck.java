package com.github.aeddddd.ae2enhanced.diag.check;

import com.github.aeddddd.ae2enhanced.dimension.PersonalDimensionData;
import com.github.aeddddd.ae2enhanced.dimension.PersonalDimensionManager;
import com.github.aeddddd.ae2enhanced.dimension.PlayerDimEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.Collection;
import java.util.List;

/**
 * 个人维度数据一致性检查.
 *
 * <ul>
 *   <li>条目维度已分配但未注册 → ERROR（孤儿条目）</li>
 *   <li>条目维度已注册但不是个人维度类型 → ERROR（ID 冲突）</li>
 *   <li>未分配维度的空条目 → 不告警（规则预编辑属正常）</li>
 * </ul>
 */
public final class PersonalDimCheck implements SystemCheck {

    @Override
    public String name() {
        return "personaldim";
    }

    @Override
    public String displayName() {
        return "个人维度";
    }

    @Override
    public void run(MinecraftServer server, List<CheckResult> out) {
        try {
            WorldServer overworld = server.getWorld(0);
            if (overworld == null) {
                out.add(CheckResult.warn("主世界不可用,跳过个人维度检查"));
                return;
            }
            PersonalDimensionData data = PersonalDimensionData.get(overworld);
            Collection<PlayerDimEntry> entries = data.getAllEntries();

            int assigned = 0;
            int unassigned = 0;
            int orphan = 0;
            int typeConflict = 0;
            for (PlayerDimEntry entry : entries) {
                int dimId = entry.dimensionId;
                if (dimId == Integer.MIN_VALUE) {
                    unassigned++;
                    continue;
                }
                assigned++;
                if (!DimensionManager.isDimensionRegistered(dimId)) {
                    orphan++;
                    out.add(CheckResult.error("玩家 " + entry.playerId + " 的维度 " + dimId
                            + " 未注册(孤儿条目)"));
                } else if (!PersonalDimensionManager.isPersonalDimension(dimId)) {
                    typeConflict++;
                    out.add(CheckResult.error("玩家 " + entry.playerId + " 的维度 " + dimId
                            + " 已被其他维度类型占用(ID 冲突)"));
                }
            }
            out.add(CheckResult.ok("条目: " + entries.size() + " 个(已分配维度 " + assigned
                    + " 个,未分配 " + unassigned + " 个)"));
            if (orphan == 0 && typeConflict == 0 && assigned > 0) {
                out.add(CheckResult.ok("维度注册状态全部一致"));
            }
        } catch (Exception e) {
            out.add(CheckResult.error("个人维度检查执行异常: " + e));
        }
    }
}
