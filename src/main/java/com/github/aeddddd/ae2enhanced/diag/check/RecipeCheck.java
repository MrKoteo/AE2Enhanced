package com.github.aeddddd.ae2enhanced.diag.check;

import com.github.aeddddd.ae2enhanced.chamber.ChamberRecipe;
import com.github.aeddddd.ae2enhanced.chamber.ChamberRecipeIndex;
import com.github.aeddddd.ae2enhanced.crafting.BlackHoleRecipeRegistry;
import com.github.aeddddd.ae2enhanced.crafting.SingularityRecipeRegistry;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 合成配方注册表健康检查：黑洞配方 / 奇点仪式配方 / 奇点处理仓索引.
 */
public final class RecipeCheck implements SystemCheck {

    @Override
    public String name() {
        return "recipes";
    }

    @Override
    public String displayName() {
        return "合成配方注册表";
    }

    @Override
    public void run(MinecraftServer server, List<CheckResult> out) {
        try {
            int blackHole = BlackHoleRecipeRegistry.getRecipes().size();
            out.add(blackHole > 0
                    ? CheckResult.ok("黑洞配方: " + blackHole + " 条")
                    : CheckResult.warn("黑洞配方注册表为空(可能被 CraftTweaker 全部移除)"));

            int singularity = SingularityRecipeRegistry.getRecipes().size();
            out.add(singularity > 0
                    ? CheckResult.ok("奇点仪式配方: " + singularity + " 条")
                    : CheckResult.warn("奇点仪式配方注册表为空(可能被 CraftTweaker 全部移除)"));

            List<ChamberRecipe> chamber = ChamberRecipeIndex.allRecipes();
            Map<String, Integer> byType = new TreeMap<>();
            for (ChamberRecipe r : chamber) {
                String prefix = r.getId().contains(":")
                        ? r.getId().substring(0, r.getId().indexOf(':')) : r.getId();
                byType.merge(prefix, 1, Integer::sum);
            }
            out.add(chamber.isEmpty()
                    ? CheckResult.error("奇点处理仓配方索引为空")
                    : CheckResult.ok("奇点处理仓配方: " + chamber.size() + " 条,byType=" + byType));
        } catch (Exception e) {
            out.add(CheckResult.error("配方检查执行异常: " + e));
        }
    }
}
