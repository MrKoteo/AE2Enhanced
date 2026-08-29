package com.github.aeddddd.ae2enhanced.mixin.bridge;

/**
 * MECraftingInventory 注入版本号访问接口.
 *
 * <p>canCraft 失败缓存（{@code MixinCraftingCPUClusterCanCraft}）依赖该版本号判断
 * 缺料状态是否可能翻转：canCraft 从 false 变为 true 的唯一途径是某个缺料输入的
 * 库存<b>增加</b>，而库存增加只会通过注入（injectItems / 产物直接入栏）发生；
 * 提取只会减少库存，false 结果不可能因此翻转。因此版本号仅随注入事件递增，
 * 发配风暴（纯提取）期间缓存保持有效，且与原生时序逐字节等价。</p>
 *
 * <p>绕过 injectItems 直接操作 {@code getItemList()} 的写入方
 * 必须在写入后显式调用 {@link #ae2e$bumpInjectVersion()}。</p>
 */
public interface IMeInventoryVersionAccess {

    /** 当前注入版本号,每次库存注入事件后递增. */
    long ae2e$getInjectVersion();

    /** 显式递增注入版本号（直接操作底层 IItemList 的产物入栏路径使用）. */
    void ae2e$bumpInjectVersion();
}
