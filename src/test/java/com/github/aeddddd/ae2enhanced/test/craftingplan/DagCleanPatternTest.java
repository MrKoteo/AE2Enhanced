package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.craftingplan.dag.DagCompiler;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ReusePatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * DagCompiler.isClean 口径回归测试（生产看门狗事故根因）.
 * <p>根因:原生 {@code PatternHelper.getSubstituteInputs} 对任何占用槽<b>恒返回
 * 非空列表</b>(至少含编码输入自身,且不看 canSubstitute 标志),旧 isClean 判空
 * 口径把所有可合成样板误判为"不干净" → 任何含工作台样板的订单整单回落原生.</p>
 * <p>修复口径:未启用替代标志 → 干净;启用时仅当存在编码输入之外的真实替代候选
 * （矿词等）才算不干净.本测试以模拟原生语义的样板（列表恒含输入自身）覆盖三态.</p>
 */
public class DagCleanPatternTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    /**
     * 回归（事故场景）:可合成样板的替代列表恒非空（含输入自身,甚至注册了候选）,
     * 但 canSubstitute=false → 执行层不会替代 → 必须判干净,DAG 正常接管.
     * 修复前：列表非空即判不干净 → unclean_inputs 整单回落原生.
     */
    @Test
    public void testNativeStyleListFlagOffIsClean() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack alt = block(Blocks.DIRT); // 注册了候选也不影响（标志关闭）
        IAEItemStack b = block(Blocks.COBBLESTONE);
        env.addPattern(new ReusePatternBuilder(b)
                .addPreciseInput(1, a)
                .nativeStyleSubstituteList()
                .substituteAlternatives(a, alt)
                .canSubstitute(false)
                .build());
        env.addStoredItem(mult(a, 1000));

        assertThatCode(() -> DagCompiler.compile(env.craftingGrid(), mult(b, 10), env.world()))
                .as("标志关闭 + 原生风格替代列表必须判干净").doesNotThrowAnyException();

        CraftingJob job = env.runDag(mult(b, 10));
        assertThat(PlanView.of(job).simulation()).as("DAG 应接管并成功").isFalse();
    }

    /**
     * 真实替代：canSubstitute=true 且存在编码输入之外的候选——
     * 自替代感知接管起不再回落:候选库存可直接顶替编码物.
     * (详细语义见 DagSubstituteTest)
     */
    @Test
    public void testRealAlternativesFlagOnAreTakenOver() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack alt = block(Blocks.DIRT);
        IAEItemStack b = block(Blocks.COBBLESTONE);
        env.addPattern(new ReusePatternBuilder(b)
                .addPreciseInput(1, a)
                .nativeStyleSubstituteList()
                .substituteAlternatives(a, alt)
                .canSubstitute(true)
                .build());
        env.addStoredItem(mult(alt, 1000)); // 只有替代品库存

        assertThatCode(() -> DagCompiler.compile(env.craftingGrid(), mult(b, 10), env.world()))
                .as("真实替代候选由替代感知接管,不再回落").doesNotThrowAnyException();

        CraftingJob job = env.runDag(mult(b, 10));
        PlanView view = PlanView.of(job);
        assertThat(view.simulation()).as("替代品库存顶替,计划可提交").isFalse();
        assertThat(view.usedItems().getOrDefault(alt, 0L)).isEqualTo(10L);
    }

    /**
     * 启用替代但无真实候选（普通单物品配料,列表只有输入自身）→ 干净,DAG 接管.
     */
    @Test
    public void testFlagOnWithoutAlternativesIsClean() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack b = block(Blocks.COBBLESTONE);
        env.addPattern(new ReusePatternBuilder(b)
                .addPreciseInput(1, a)
                .nativeStyleSubstituteList() // 列表 = [输入自身],无额外候选
                .canSubstitute(true)
                .build());
        env.addStoredItem(mult(a, 1000));

        assertThatCode(() -> DagCompiler.compile(env.craftingGrid(), mult(b, 10), env.world()))
                .as("无真实替代候选必须判干净").doesNotThrowAnyException();

        CraftingJob job = env.runDag(mult(b, 10));
        assertThat(PlanView.of(job).simulation()).as("DAG 应接管并成功").isFalse();
    }
}
