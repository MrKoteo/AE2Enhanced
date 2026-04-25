package com.github.aeddddd.ae2enhanced.item;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.item.Item;

/**
 * 微分形式稳定单元 —— 黑洞退火产物，T2 材料。
 */
public class ItemDifferentialFormStabilizer extends Item {

    public ItemDifferentialFormStabilizer() {
        setRegistryName(AE2Enhanced.MOD_ID, "differential_form_stabilizer");
        setTranslationKey(AE2Enhanced.MOD_ID + ".differential_form_stabilizer");
        setCreativeTab(null);
    }
}
