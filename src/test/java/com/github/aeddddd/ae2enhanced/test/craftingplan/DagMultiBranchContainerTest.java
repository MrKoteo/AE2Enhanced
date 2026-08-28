package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ReusePatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * 多样板 + 容器/返还输入(container_multi 解封)回归测试.
 * <p>旧实现:多样板节点任一分支含容器/返还输入 → 整单回落原生.
 * 现由执行器逐分支返还记账(自返还 times-1 保种子,跨样板全额),
 * 与原生逐分支逐次循环语义等价.</p>
 */
public class DagMultiBranchContainerTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    /**
     * 同一物品两块样板,分支 1 的输入为配方级不消耗(CrT reuse)——
     * 多样板 + 返还输入不再回落;分支 1 包揽全部需求,自返还 times-1 记账:
     * 提取 5、返还 4、网络净消耗 = 1(种子).
     */
    @Test
    public void testMultiBranchWithReusedInput() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack y = block(Blocks.COBBLESTONE);
        // 分支 1:1A(reuse)→1Y;分支 2:1A→1Y(普通消耗)
        appeng.api.networking.crafting.ICraftingPatternDetails p1 = new ReusePatternBuilder(y)
                .addPreciseInput(1, a).reused(a).build();
        appeng.api.networking.crafting.ICraftingPatternDetails p2 = new ReusePatternBuilder(y)
                .addPreciseInput(1, a).build();
        env.addPattern(p1);
        env.addPattern(p2);
        env.addStoredItem(mult(a, 10));

        CraftingJob job = env.runDag(mult(y, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).as("多样板+返还输入应走 DAG 成功").isFalse();
        assertThat(view.missingItems()).isEmpty();
        assertThat(view.patternTimes().getOrDefault(p1, -1L))
                .as("分支 1 包揽(分支序 = 原生尝试序)").isEqualTo(5L);
        // 分支 1 自返还 times-1:5 次消耗提取 5,回记 4 → used = 净消耗 1 + 种子 4? 
        // 记账口径:used = 网络实取 = 5(返还经 synthetic 抵扣模拟库存,不再重复实取)
        assertThat(view.usedItems().getOrDefault(a, 0L)).isEqualTo(5L);
    }

    /**
     * 分支 1 容量不足时落到分支 2(普通消耗),两分支的返还/消耗各自记账.
     */
    @Test
    public void testMultiBranchCapacitySpillover() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack c = block(Blocks.DIRT);
        IAEItemStack y = block(Blocks.COBBLESTONE);
        // 分支 1:1A(reuse)→1Y(A 只有 3 个库存,容量封顶);分支 2:1C→1Y
        appeng.api.networking.crafting.ICraftingPatternDetails p1 = new ReusePatternBuilder(y)
                .addPreciseInput(1, a).reused(a).build();
        appeng.api.networking.crafting.ICraftingPatternDetails p2 = new ReusePatternBuilder(y)
                .addPreciseInput(1, c).build();
        env.addPattern(p1);
        env.addPattern(p2);
        env.addStoredItem(mult(a, 3));
        env.addStoredItem(mult(c, 100));

        CraftingJob job = env.runDag(mult(y, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).isFalse();
        assertThat(view.missingItems()).isEmpty();
        assertThat(view.patternTimes().getOrDefault(p1, -1L)).isEqualTo(3L);
        assertThat(view.patternTimes().getOrDefault(p2, -1L)).isEqualTo(2L);
        assertThat(view.usedItems().getOrDefault(a, 0L)).isEqualTo(3L);
        assertThat(view.usedItems().getOrDefault(c, 0L)).isEqualTo(2L);
    }
}
