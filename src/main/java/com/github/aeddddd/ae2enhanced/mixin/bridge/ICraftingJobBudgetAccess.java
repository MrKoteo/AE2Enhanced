package com.github.aeddddd.ae2enhanced.mixin.bridge;

/**
 * CraftingJob 原生路径计算预算访问器（由本模组 job 类直接实现,
 * MixinCraftingJob 仅在其 handlePausing 心跳上调用检查逻辑）.
 * <p>用途:DAG/特殊求解器回落原生计算时挂上时间预算——原生递归在病态计划
 * （数万节点 × 大数量,含多样板逐个合成循环）上可能永不结束,而下单流程中
 * RandomComplement 等模组会在 setJob 里同步 future.get() 阻塞服务器线程,
 * 直接触发看门狗.超预算即借原生取消语义中断计算并把计划钉为模拟态.</p>
 * <p>注:状态由 job 类自持（而非 mixin 字段）,JUnit 环境（无 mixin 变换）
 * 也能驱动机制逻辑.</p>
 */
public interface ICraftingJobBudgetAccess {

    /** 原生计算预算的绝对截止时间（System.nanoTime 域;0 = 未挂载）. */
    long ae2enhanced$nativeCalcDeadlineNanos();

    /** 挂载原生计算预算. */
    void ae2enhanced$armNativeCalcBudget(long deadlineNanos);

    /** 标记本次原生计算已因超预算中断（由预算检查在抛出中断前调用）. */
    void ae2enhanced$markNativeCalcAborted();

    /** 本次原生计算是否因超预算被中断. */
    boolean ae2enhanced$nativeCalcAborted();
}
