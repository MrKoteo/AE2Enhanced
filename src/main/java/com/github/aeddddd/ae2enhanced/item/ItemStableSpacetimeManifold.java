package com.github.aeddddd.ae2enhanced.item;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.item.Item;

/**
 * 稳态时空流形 —— 黑洞退火产物，T1 材料。
 */
public class ItemStableSpacetimeManifold extends Item {

    public ItemStableSpacetimeManifold() {
        setRegistryName(AE2Enhanced.MOD_ID, "stable_spacetime_manifold");
        setTranslationKey(AE2Enhanced.MOD_ID + ".stable_spacetime_manifold");
        setCreativeTab(null);
    }
}
