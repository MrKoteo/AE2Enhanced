package com.github.aeddddd.ae2enhanced.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * 超因果计算核心控制器 TileEntity。
 * 后续将实现 IGridProxyable + ICraftingProvider + ICraftingMedium。
 */
public class TileComputationCore extends TileEntity {

    private boolean formed = false;

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.formed = compound.getBoolean("formed");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("formed", formed);
        return compound;
    }
}
