package com.github.aeddddd.ae2enhanced.integration.jei;

import com.github.aeddddd.ae2enhanced.crafting.BlackHoleRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI 黑洞合成配方包装器。
 */
public class BlackHoleRecipeWrapper implements IRecipeWrapper {

    private final BlackHoleRecipe recipe;

    public BlackHoleRecipeWrapper(BlackHoleRecipe recipe) {
        this.recipe = recipe;
    }

    public BlackHoleRecipe getRecipe() {
        return recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (Map.Entry<net.minecraft.item.Item, Integer> entry : recipe.getInputs().entrySet()) {
            List<ItemStack> subList = new ArrayList<>();
            subList.add(new ItemStack(entry.getKey(), entry.getValue()));
            inputs.add(subList);
        }
        ingredients.setInputLists(ItemStack.class, inputs);
        ingredients.setOutput(ItemStack.class, recipe.getOutput());
    }
}
