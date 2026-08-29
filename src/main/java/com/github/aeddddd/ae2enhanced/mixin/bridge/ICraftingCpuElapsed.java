package com.github.aeddddd.ae2enhanced.mixin.bridge;

/**
 * CraftingCPUStatus 的已持续时间访问桥.
 * <p>由 MixinCraftingCPUStatus 实现: 服务端构造时从 CraftingCPUCluster 捕获
 * getElapsedTime() 并随 NBT 同步到客户端, 供 CPU 选择列表显示各任务已耗时.</p>
 */
public interface ICraftingCpuElapsed {

    /** 该 CPU 当前任务的已持续时间(纳秒), 空闲或未知时为 0. */
    long ae2enhanced$getElapsedTimeNanos();
}
