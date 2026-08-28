package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanAssert.assertThatPlan;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.CycleAnalyzer;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * 大量相互交叉的增产环场景（多循环相互依赖、环键互为共输入）.
 * <p>关键语义:环内每个样板自身都是"净增殖自引用"(dup)——单独看可增产,
 * 但共输入是同环成员时必须并集联立;单键贷款法(路径①)会把共输入当普通外部
 * 输入经原生树请求,而原生 notRecursive 排除祖先样板,必然部分失败误记缺料.</p>
 */
public class DagCrossCycleTest {

    private static IAEItemStack item(net.minecraft.item.Item i) {
        return AEItemStack.fromItemStack(new ItemStack(i));
    }

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    private static void dump(String name, PlanView view, long elapsedMs) {
        System.out.printf("[XCYCLE] %s: %d ms, simulation=%s%n", name, elapsedMs, view.simulation());
        for (Map.Entry<IAEItemStack, Long> e : view.usedItems().entrySet()) {
            System.out.printf("[XCYCLE]   used %s = %d%n", e.getKey(), e.getValue());
        }
        for (Map.Entry<IAEItemStack, Long> e : view.missingItems().entrySet()) {
            System.out.printf("[XCYCLE]   missing %s = %d%n", e.getKey(), e.getValue());
        }
        for (Map.Entry<ICraftingPatternDetails, Long> e : view.patternTimes().entrySet()) {
            StringBuilder outs = new StringBuilder();
            for (IAEItemStack o : e.getKey().getCondensedOutputs()) {
                if (o != null) {
                    outs.append(o).append('×').append(o.getStackSize()).append(' ');
                }
            }
            System.out.printf("[XCYCLE]   pattern [%s] × %d%n", outs, e.getValue());
        }
    }

    /**
     * ① 两键互依增产环:A 增产吃 B(1A+1B→2A),B 增产吃 A(1A+1B→3B).
     * 单键看都是净增殖自引用;联立后 t=[1,2],每超轮净产 1A——数学上可解.
     * 回归防护:路径① dup 快速路径必须让位于②并集联立,且前缀种子须覆盖
     * 自引用步骤的毛额输入下探(A 种子 3、B 种子 1).
     */
    @Test
    public void testMutualProductiveDupCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = item(Items.DIAMOND);
        IAEItemStack b = item(Items.EMERALD);
        IAEItemStack r = block(Blocks.STONE);
        ICraftingPatternDetails pa = env.addPattern(new ProcessingPatternBuilder(mult(a, 2))
                .addPreciseInput(1, a).addPreciseInput(1, b).build());
        ICraftingPatternDetails pb = env.addPattern(new ProcessingPatternBuilder(mult(b, 3))
                .addPreciseInput(1, a).addPreciseInput(1, b).build());
        ICraftingPatternDetails pr = env.addPattern(
                new ProcessingPatternBuilder(r).addPreciseInput(1, a).build());
        env.addStoredItem(mult(a, 4)); // 库存 ≥ 每超轮种子(A=3)
        env.addStoredItem(mult(b, 4)); // 库存 ≥ 每超轮种子(B=1)

        long t0 = System.nanoTime();
        CraftingJob job = env.runDag(mult(r, 5));
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        PlanView view = PlanView.of(job);
        dump("mutual-productive-dup", view, elapsed);

        // 请求 5A → 5 超轮:P_B×5、P_A×10,外加根样板 R×5
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(pr, 5L);
        expected.put(pb, 5L);
        expected.put(pa, 10L);
        assertThatPlan(view).succeeded().patternsMatch(expected).missingMatch();
    }

    /**
     * ① 的分析器层断言:两键互依增产环环结构、t 比、毛额下探种子.
     */
    @Test
    public void testMutualProductiveDupCycleAnalysis() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = item(Items.DIAMOND);
        IAEItemStack b = item(Items.EMERALD);
        env.addPattern(new ProcessingPatternBuilder(mult(a, 2))
                .addPreciseInput(1, a).addPreciseInput(1, b).build());
        env.addPattern(new ProcessingPatternBuilder(mult(b, 3))
                .addPreciseInput(1, a).addPreciseInput(1, b).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer
                .findCyclesThrough(env.craftingGrid(), a, null);
        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0)).hasSize(2);

        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.PRODUCTIVE);
        assertThat(analysis.timesPerRound()).containsExactly(1, 2); // 步骤序 [P_B, P_A]
        assertThat(analysis.netGain()).isEqualTo(1);
        // A 种子 3:P_B×1 净消费 1A 后,P_A×2 批量需毛额 2A 自我输入(下探至 -3);
        // B 种子 1:P_B×1 批量需毛额 1B 自我输入
        assertThat(analysis.seedsPerKey()).containsExactly(3, 1);
    }

    /**
     * ② 致密交叉增产网:K 键环形 + 跨边,每键一个增产样板吃相邻两键
     * (1K_i + 1K_{i+1} + 1K_{i+3} → 3K_i;root 键样板产 4 制造盈余).
     * 候选环数量随交叉边组合增长,平衡解 t 全 1,每超轮净产 1K_0——数学上可解.
     */
    @Test
    public void testDenseCrossLinkedProductiveWeb() {
        SimulationEnv env = new SimulationEnv();
        int k = 6;
        IAEItemStack[] keys = new IAEItemStack[k];
        for (int i = 0; i < k; i++) {
            keys[i] = AEItemStack.fromItemStack(new ItemStack(Items.STICK, 1, 100 + i));
        }
        ICraftingPatternDetails[] patterns = new ICraftingPatternDetails[k];
        for (int i = 0; i < k; i++) {
            // 键 i 增产:1K_i + 1K_{(i+1)%k} + 1K_{(i+3)%k} → out K_i(跨边制造大量交叉环);
            // root 键 K_0 产出 4(净 +3),其余产出 3(净 +2),环内盈余汇聚到 root
            patterns[i] = env.addPattern(new ProcessingPatternBuilder(mult(keys[i], i == 0 ? 4 : 3))
                    .addPreciseInput(1, keys[i])
                    .addPreciseInput(1, keys[(i + 1) % k])
                    .addPreciseInput(1, keys[(i + 3) % k])
                    .build());
        }
        IAEItemStack r = block(Blocks.COBBLESTONE);
        ICraftingPatternDetails pr = env.addPattern(
                new ProcessingPatternBuilder(r).addPreciseInput(1, keys[0]).build());
        for (IAEItemStack key : keys) {
            env.addStoredItem(mult(key, 8)); // 种子充足
        }

        long t0 = System.nanoTime();
        CraftingJob job = env.runDag(mult(r, 5));
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        PlanView view = PlanView.of(job);
        dump("dense-web-k6", view, elapsed);

        // 请求 5K_0 → 5 超轮 × t 全 1:每个环样板 ×5,外加根样板 R×5
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(pr, 5L);
        for (ICraftingPatternDetails p : patterns) {
            expected.put(p, 5L);
        }
        assertThatPlan(view).succeeded().patternsMatch(expected).missingMatch();
    }

    /**
     * ③ 四键闭环互依增产链:A 增产吃 B,B 吃 C,C 吃 D,D 吃 A(全部 1X+1next→2X).
     * 单键看全部净增殖,但闭环无外部注入:每超轮各键净变化恒为 0(中性守恒环),
     * 数学上不可行——必须正确判缺料,不得误判可合成,也不得卡死.
     */
    @Test
    public void testClosedConservationChainCorrectlyRejected() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = item(Items.DIAMOND);
        IAEItemStack b = item(Items.EMERALD);
        IAEItemStack c = item(Items.IRON_INGOT);
        IAEItemStack d = item(Items.GOLD_INGOT);
        IAEItemStack r = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(mult(a, 2)).addPreciseInput(1, a).addPreciseInput(1, b).build());
        env.addPattern(new ProcessingPatternBuilder(mult(b, 2)).addPreciseInput(1, b).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(c, 2)).addPreciseInput(1, c).addPreciseInput(1, d).build());
        env.addPattern(new ProcessingPatternBuilder(mult(d, 2)).addPreciseInput(1, d).addPreciseInput(1, a).build());
        env.addPattern(new ProcessingPatternBuilder(r).addPreciseInput(1, a).build());
        env.addStoredItem(a);
        env.addStoredItem(b);
        env.addStoredItem(c);
        env.addStoredItem(d);

        long t0 = System.nanoTime();
        CraftingJob job = env.runDag(mult(r, 5));
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        PlanView view = PlanView.of(job);
        dump("closed-conservation-chain", view, elapsed);

        assertThatPlan(view).failed();
        assertThat(view.missingItems()).isNotEmpty();
    }
}
