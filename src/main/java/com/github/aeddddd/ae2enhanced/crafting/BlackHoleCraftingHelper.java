package com.github.aeddddd.ae2enhanced.crafting;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑洞合成辅助类。
 * 扫描黑洞中心周围 3×3×3 区域内的物品实体，累加匹配配方后消耗/产出。
 */
public class BlackHoleCraftingHelper {

    public static void tryCraft(World world, BlockPos pos) {
        AxisAlignedBB area = new AxisAlignedBB(
                pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2
        );
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area);
        if (items.isEmpty()) return;

        // 累加物品数量
        Map<Item, Integer> found = new HashMap<>();
        for (EntityItem entityItem : items) {
            ItemStack stack = entityItem.getItem();
            if (stack.isEmpty()) continue;
            found.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        // 匹配配方
        BlackHoleRecipe recipe = BlackHoleRecipeRegistry.findMatching(found);
        if (recipe != null) {
            // 消耗材料
            Map<Item, Integer> remaining = new HashMap<>(recipe.getInputs());
            for (EntityItem entityItem : items) {
                ItemStack stack = entityItem.getItem();
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                int needed = remaining.getOrDefault(item, 0);
                if (needed > 0) {
                    int consume = Math.min(needed, stack.getCount());
                    stack.shrink(consume);
                    remaining.put(item, needed - consume);
                    if (stack.isEmpty()) {
                        entityItem.setDead();
                    }
                }
            }
            // 生成产物（从黑洞中心向上喷出）
            EntityItem result = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    recipe.getOutput().copy());
            result.setNoPickupDelay();
            world.spawnEntity(result);
        } else {
            // 不匹配任何配方：黑洞销毁所有物品
            for (EntityItem entityItem : items) {
                entityItem.setDead();
            }
        }
    }
}
