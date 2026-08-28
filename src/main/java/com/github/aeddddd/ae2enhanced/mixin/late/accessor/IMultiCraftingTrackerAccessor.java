package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import appeng.helpers.MultiCraftingTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * MultiCraftingTracker 的包私有 isBusy(int) 访问接口.
 * 供 MixinDualityInterface 的 usePlan 重排逻辑调用.
 */
@Mixin(value = MultiCraftingTracker.class, remap = false)
public interface IMultiCraftingTrackerAccessor {

    @Invoker("isBusy")
    boolean ae2e$invokeIsBusy(int slot);
}
