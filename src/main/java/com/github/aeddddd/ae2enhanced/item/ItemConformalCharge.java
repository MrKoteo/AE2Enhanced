package com.github.aeddddd.ae2enhanced.item;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.item.Item;

/**
 * 共形不变荷 —— 黑洞退火产物，T3 材料。
 */
public class ItemConformalCharge extends Item {

    public ItemConformalCharge() {
        setRegistryName(AE2Enhanced.MOD_ID, "conformal_invariant_charge");
        setTranslationKey(AE2Enhanced.MOD_ID + ".conformal_invariant_charge");
        setCreativeTab(null); // 不显示在创造模式物品栏，通过黑洞合成获得
    }
}
