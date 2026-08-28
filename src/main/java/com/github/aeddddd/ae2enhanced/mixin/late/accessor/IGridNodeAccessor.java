package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.networking.IGridConnection;
import appeng.me.GridNode;

/**
 * GridNode 包私有成员访问接口。
 * 用于 GridValidationBatcher 的网格分裂检测批处理。
 * 目标为 AE2 本家类，始终存在，remap=false 使用源码名。
 */
@Mixin(value = GridNode.class, remap = false)
public interface IGridNodeAccessor {

    @Invoker("removeConnection")
    void ae2e$removeConnection(IGridConnection connection);

    @Invoker("validateGrid")
    void ae2e$validateGrid();
}
