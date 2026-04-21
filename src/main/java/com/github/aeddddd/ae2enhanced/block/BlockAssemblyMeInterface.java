package com.github.aeddddd.ae2enhanced.block;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockAssemblyMeInterface extends Block {

    public BlockAssemblyMeInterface() {
        super(Material.IRON);
        setRegistryName(AE2Enhanced.MOD_ID, "assembly_me_interface");
        setTranslationKey(AE2Enhanced.MOD_ID + ".assembly_me_interface");
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2);
    }
}
