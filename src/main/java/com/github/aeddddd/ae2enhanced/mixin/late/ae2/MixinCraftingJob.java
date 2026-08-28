package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.crafting.CraftingJob;

import com.github.aeddddd.ae2enhanced.specialcrafting.NativeCalcBudget;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanDisplayHook;

/**
 * 原生合成计算完成钩子:为普通计划补充"样板调用 N 次"显示信息
 * （特殊计划的完整信息由 SpecialCraftingJob 自行发送——其子类 override run()
 * 不会被本注入覆盖）.
 * <p>另挂载<b>原生路径计算预算</b>的心跳检查:handlePausing 是原生逐节点/逐子请求的
 * 高频心跳,委托 {@link NativeCalcBudget#checkDeadline}——超预算先钉模拟态
 * (不完整计划绝不允许被提交),再抛 InterruptedException 借原生取消语义收尾.
 * 预算状态由本模组 job 类自持(ICraftingJobBudgetAccess);普通原生 job
 * 未实现该接口,零开销零影响.</p>
 */
@Mixin(value = CraftingJob.class, remap = false)
public class MixinCraftingJob {

    @Inject(method = "run", at = @At("RETURN"), require = 0)
    private void ae2enhanced$sendCallCounts(CallbackInfo ci) {
        SpecialPlanDisplayHook.sendCallCounts((CraftingJob) (Object) this);
    }

    @Inject(method = "handlePausing", at = @At("HEAD"), require = 0)
    private void ae2enhanced$nativeCalcBudgetCheck(CallbackInfo ci) throws InterruptedException {
        NativeCalcBudget.checkDeadline((CraftingJob) (Object) this);
    }
}
