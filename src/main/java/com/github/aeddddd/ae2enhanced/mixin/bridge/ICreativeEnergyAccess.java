package com.github.aeddddd.ae2enhanced.mixin.bridge;

import appeng.api.networking.energy.IEnergyGrid;

/**
 * CraftingCPUCluster 的创造能源检测访问接口.
 * 由 {@code MixinCraftingCPUClusterCreativeEnergy} 实现。
 */
public interface ICreativeEnergyAccess {

    /**
     * 网络中是否存在创造能源元件（结果按世界 tick 缓存 100 tick）.
     */
    boolean ae2e$hasCreativeEnergy(IEnergyGrid eg);
}
