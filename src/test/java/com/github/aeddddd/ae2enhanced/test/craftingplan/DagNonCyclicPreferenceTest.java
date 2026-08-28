package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * 分层选样板(少环配方优先)测试.
 * <p>背景:多个循环配方互相嵌套时计划大幅膨胀(边界子树乘性展开).
 * 编译期在同一优先级层内优先选择非环样板,避开本可避免的循环边界;
 * 显式优先级层不被跨越——最高优先级层全部成环时才收缩为循环边界
 * (用户可用样板优先级强制走环配方).</p>
 */
public class DagNonCyclicPreferenceTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    /**
     * 同优先级层内:环样板(自增殖 1Y→2Y)排在前面,非环样板(1X→1Y)在后——
     * 必须选非环,不产生循环边界.
     */
    @Test
    public void testPrefersNonCyclicWithinSameTier() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack y = block(Blocks.COBBLESTONE);
        IAEItemStack x = block(Blocks.STONE);
        ICraftingPatternDetails pCycle = new ProcessingPatternBuilder(mult(y, 2))
                .addPreciseInput(1, y).build(); // 自增殖环步骤,故意先插入
        ICraftingPatternDetails pNormal = new ProcessingPatternBuilder(y)
                .addPreciseInput(1, x).build();
        env.addPattern(pCycle);
        env.addPattern(pNormal);
        env.addStoredItem(mult(x, 100));

        CraftingJob job = env.runDag(mult(y, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).isFalse();
        assertThat(view.patternTimes().getOrDefault(pNormal, 0L))
                .as("同层优先非环样板").isEqualTo(5L);
        assertThat(view.patternTimes().getOrDefault(pCycle, 0L))
                .as("环样板不被选用").isEqualTo(0L);
    }

    /**
     * 环样板作为额外分支(主分支非环)不再触发 cycle_multi 整单回落.
     */
    @Test
    public void testCyclicBranchExcludedNoFallback() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack y = block(Blocks.COBBLESTONE);
        IAEItemStack x = block(Blocks.STONE);
        ICraftingPatternDetails pNormal = new ProcessingPatternBuilder(y)
                .addPreciseInput(1, x).build();
        ICraftingPatternDetails pCycle = new ProcessingPatternBuilder(mult(y, 2))
                .addPreciseInput(1, y).build();
        env.addPattern(pNormal);
        env.addPattern(pCycle);
        env.addStoredItem(mult(x, 100));

        CraftingJob job = env.runDag(mult(y, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).as("环分支静默排除,不再整单回落").isFalse();
        assertThat(view.patternTimes().getOrDefault(pNormal, 0L)).isEqualTo(5L);
    }

    /**
     * 最高优先级层全部成环 → 仍收缩为循环边界(保留边界求解能力).
     * (环键作为子节点——根级自引用由 detector 路由 SpecialCraftingJob,不经 DAG)
     */
    @Test
    public void testAllCyclicTopTierStillBoundary() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack y = block(Blocks.COBBLESTONE);
        IAEItemStack r = block(Blocks.DIRT);
        ICraftingPatternDetails pCycle = new ProcessingPatternBuilder(mult(y, 2))
                .addPreciseInput(1, y).build();
        env.addPattern(pCycle);
        env.addPattern(new ProcessingPatternBuilder(r).addPreciseInput(1, y).build());
        env.addStoredItem(y); // 种子 1

        CraftingJob job = env.runDag(mult(r, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).as("全成环时边界求解照常").isFalse();
        assertThat(view.patternTimes().getOrDefault(pCycle, 0L))
                .as("自增殖边界约定:次数 = 需求额(种子仅作贷款担保,对齐 Z1)").isEqualTo(5L);
    }

    /**
     * 显式优先级不被跨越:环样板优先级更高时尊重用户选择,仍走循环边界,
     * 低优先级的非环样板不启用(用户可用优先级强制环配方).
     */
    @Test
    public void testHigherPriorityCycleRespected() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack y = block(Blocks.COBBLESTONE);
        IAEItemStack x = block(Blocks.STONE);
        IAEItemStack r = block(Blocks.DIRT);
        ICraftingPatternDetails pCycle = new ProcessingPatternBuilder(mult(y, 2))
                .addPreciseInput(1, y).withPriority(5).build();
        ICraftingPatternDetails pNormal = new ProcessingPatternBuilder(y)
                .addPreciseInput(1, x).withPriority(0).build();
        env.addPattern(pCycle);
        env.addPattern(pNormal);
        env.addPattern(new ProcessingPatternBuilder(r).addPreciseInput(1, y).build());
        env.addStoredItem(y); // 种子 1
        env.addStoredItem(mult(x, 100));

        CraftingJob job = env.runDag(mult(r, 5));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).isFalse();
        assertThat(view.patternTimes().getOrDefault(pCycle, 0L))
                .as("高优先级环配方被尊重").isEqualTo(5L);
        assertThat(view.patternTimes().getOrDefault(pNormal, 0L))
                .as("低优先级非环不跨层启用").isEqualTo(0L);
    }
}
