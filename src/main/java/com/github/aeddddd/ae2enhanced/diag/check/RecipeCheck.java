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

    private static final String KEY_PREFIX = "chat.ae2enhanced.check.recipes.";

    @Override
    public String name() {
        return "recipes";
    }

    @Override
    public String displayName() {
        return KEY_PREFIX + "name";
    }

    @Override
    public void run(MinecraftServer server, List<CheckResult> out) {
        try {
            int blackHole = BlackHoleRecipeRegistry.getRecipes().size();
            out.add(blackHole > 0
                    ? CheckResult.ok(KEY_PREFIX + "blackhole_ok", blackHole)
                    : CheckResult.warn(KEY_PREFIX + "blackhole_empty"));

            int singularity = SingularityRecipeRegistry.getRecipes().size();
            out.add(singularity > 0
                    ? CheckResult.ok(KEY_PREFIX + "ritual_ok", singularity)
                    : CheckResult.warn(KEY_PREFIX + "ritual_empty"));

            List<ChamberRecipe> chamber = ChamberRecipeIndex.allRecipes();
            Map<String, Integer> byType = new TreeMap<>();
            for (ChamberRecipe r : chamber) {
                String prefix = r.getId().contains(":")
                        ? r.getId().substring(0, r.getId().indexOf(':')) : r.getId();
                byType.merge(prefix, 1, Integer::sum);
            }
            out.add(chamber.isEmpty()
                    ? CheckResult.error(KEY_PREFIX + "chamber_empty")
                    : CheckResult.ok(KEY_PREFIX + "chamber_ok", chamber.size(), byType));
        } catch (Exception e) {
            out.add(CheckResult.error(KEY_PREFIX + "exception", String.valueOf(e)));
        }
    }
}
