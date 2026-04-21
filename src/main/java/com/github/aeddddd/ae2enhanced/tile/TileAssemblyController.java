package com.github.aeddddd.ae2enhanced.tile;

import net.minecraft.tileentity.TileEntity;

public class TileAssemblyController extends TileEntity {

    private boolean formed = false;

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
        markDirty();
    }

    public void assemble() {
        if (!formed) {
            formed = true;
            markDirty();
            // TODO: AE 网络初始化（M3 实现）
        }
    }

    public void disassemble() {
        if (formed) {
            formed = false;
            markDirty();
            // TODO: AE 网络销毁（M3 实现）
        }
    }
}
