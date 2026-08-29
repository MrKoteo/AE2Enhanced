package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.github.aeddddd.ae2enhanced.mixin.bridge.IMeInventoryVersionAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;

/**
 * canCraft 失败缓存（缺料 stall 场景优化）.
 *
 * <p>背景：AE2-UEL 的 executeCrafting 每 tick 对每个未完成 task 调用一次
 * {@code canCraft(details, condensedInputs)}，其内部对全部输入做 SIMULATE 提取
 * （findPrecise/findFuzzy + NBT 比较）。缺料期间结果恒为 false，属于纯重复计算
 * （spark 采样占服务端 tick ~20%），且每次模拟产生的临时对象是大型订单下
 * GC 压力的主要来源。</p>
 *
 * <p>语义等价性论证：canCraft 是 (details, CPU 本地库存) 的纯函数——
 * 不访问网络库存、不检查能量、不访问 medium 状态。其 false → true 翻转的唯一途径
 * 是某个缺料输入的库存<b>增加</b>，而库存增加只会通过注入事件（injectItems /
 * 产物直接入栏）发生；提取只会减少库存，不可能使 false 翻转。因此以
 * <b>注入版本号</b>（{@link IMeInventoryVersionAccess}，仅随注入事件递增）为缓存键：
 * 命中时直接返回 false 与重算结果逐字节一致，且发配风暴（纯提取）期间缓存持续有效。
 * storeItems/submitJob 回滚等绕过版本号的写入路径在此显式清空缓存。</p>
 *
 * <p>priority 900（低于 Special 的 1000）：使 vetoPushOverQuota 包装在外层，
 * 每 tick 的配额判定先于本缓存执行，不被短路。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 900)
public abstract class MixinCraftingCPUClusterCanCraft {

    /** details 实例 -> 上次 canCraft 返回 false 时的注入版本号. */
    @Unique
    private final IdentityHashMap<ICraftingPatternDetails, Long> ae2enhanced$canCraftFailCache =
        new IdentityHashMap<>();

    @Shadow
    public abstract IMEInventory<IAEItemStack> getInventory();

    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;canCraft(Lappeng/api/networking/crafting/ICraftingPatternDetails;[Lappeng/api/storage/data/IAEItemStack;)Z"
        ),
        require = 0
    )
    private boolean ae2enhanced$cacheCanCraftFailure(CraftingCPUCluster self, ICraftingPatternDetails details,
            IAEItemStack[] condensedInputs, Operation<Boolean> original) {
        IMEInventory<IAEItemStack> inv = this.getInventory();
        if (!(inv instanceof IMeInventoryVersionAccess)) {
            return original.call(self, details, condensedInputs);
        }
        long version = ((IMeInventoryVersionAccess) inv).ae2e$getInjectVersion();
        Long failedAt = ae2enhanced$canCraftFailCache.get(details);
        if (failedAt != null && failedAt.longValue() == version) {
            // 自上次失败后未发生任何注入：缺料状态不可能翻转，canCraft 必然仍返回 false
            return false;
        }
        boolean result = original.call(self, details, condensedInputs);
        if (result) {
            ae2enhanced$canCraftFailCache.remove(details);
        } else {
            ae2enhanced$canCraftFailCache.put(details, version);
        }
        return result;
    }

    /** storeItems 直接改写堆叠数量并可能整体替换 inventory 实例,绕过版本号,需显式失效. */
    @Inject(method = "storeItems", at = @At("HEAD"), require = 0)
    private void ae2enhanced$clearFailCacheOnStoreItems(CallbackInfo ci) {
        ae2enhanced$canCraftFailCache.clear();
    }

    /** submitJob 失败回滚路径直接 getItemList().resetStatus() 清零,绕过版本号,需显式失效. */
    @Inject(method = "submitJob", at = @At("RETURN"), require = 0)
    private void ae2enhanced$clearFailCacheOnSubmitJob(IGrid g, ICraftingJob job, IActionSource src,
            ICraftingRequester requestingMachine, CallbackInfoReturnable<ICraftingLink> cir) {
        ae2enhanced$canCraftFailCache.clear();
    }
}
