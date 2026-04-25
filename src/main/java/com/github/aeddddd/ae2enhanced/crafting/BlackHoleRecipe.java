package com.github.aeddddd.ae2enhanced.crafting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 黑洞合成配方。
 * 物品被投入黑洞事件视界后，若累计数量满足输入，则转化为输出产物。
 */
public class BlackHoleRecipe {

    private final String id;
    private final Map<Item, Integer> inputs;
    private final ItemStack output;

    public BlackHoleRecipe(String id, Map<Item, Integer> inputs, ItemStack output) {
        this.id = id;
        this.inputs = new HashMap<>(inputs);
        this.output = output.copy();
    }

    public String getId() {
        return id;
    }

    public Map<Item, Integer> getInputs() {
        return new HashMap<>(inputs);
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    /**
     * 检查 found 中是否包含所有输入且数量足够。
     */
    public boolean matches(Map<Item, Integer> found) {
        for (Map.Entry<Item, Integer> entry : inputs.entrySet()) {
            if (found.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
