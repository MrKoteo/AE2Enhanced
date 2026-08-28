package com.github.aeddddd.ae2enhanced.craftingplan.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;
import com.github.aeddddd.ae2enhanced.specialcrafting.CycleAnalyzer;
import com.github.aeddddd.ae2enhanced.specialcrafting.RecipeRemainingResolver;
import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;

/**
 * DAG 编译器:从样板索引把请求展开为计划图（1.12.2 移植）.
 * <ul>
 * <li>节点按 key 合并——重复子树只编译一次(相对原生递归树的核心提速点);</li>
 * <li>两遍编译:第一遍 DFS 探环(回边目标记入边界集合),第二遍把边界 key
 * 当 CYCLE 叶子正式编译;出现边界外的新环 → 回落;</li>
 * <li>替代感知:启用替代标志的样板照常接管——边携带逐槽替代候选(矿词等),
 * 执行器需求拆分提取(编码优先→候选交集),与原生"替代仅作用于库存提取、
 * 合成仍走编码物"语义一致;</li>
 * <li>分层选样板:同优先级层内优先非环样板(少环配方,抑制嵌套环计划膨胀);
 * 最高优先级层全部成环才收缩为循环边界,由 CycleBoundarySolver 联立求解;</li>
 * <li>预算:节点数上限,超限即回落(防病态网络卡死计算线程).</li>
 * </ul>
 */
public final class DagCompiler {

    /** 单图节点数上限(病态深度/广度保护;生产大单可到 14w+,经配置放宽). */
    public static final int MAX_NODES = 100_000;

    /** 实际生效的节点上限(配置优先,保底 MAX_NODES 常量值). */
    private static int maxNodes() {
        return AE2EnhancedConfig.crafting == null ? MAX_NODES
                : Math.max(1, AE2EnhancedConfig.crafting.dagMaxNodes);
    }

    private static final int WHITE = 0;
    private static final int GRAY = 1;
    private static final int BLACK = 2;

    private final ICraftingGrid cc;
    private final World world;
    private final Map<IAEItemStack, DagGraph.DagNode> nodes = new HashMap<>();
    private final Map<IAEItemStack, Integer> colors = new HashMap<>();
    private final List<DagGraph.DagNode> postOrder = new ArrayList<>();
    /** 探环遍历时发现的边界 key(回边目标);第二遍编译把它们当叶子. */
    private final Set<IAEItemStack> boundaryKeys;
    /** true = 第一遍(只探环,容忍回落);false = 第二遍(正式编译). */
    private final boolean detectOnly;
    /** 两遍编译共享的生产者索引(成环检测的副产物倒排只建一次;SCC 快路径经其共享索引). */
    private final CycleAnalyzer.ProducerIndex producerIndex;
    /** 本趟编译是否产出了多样板节点(传导到 DagGraph.hasMultiBranch). */
    private boolean sawMultiBranch;

    private DagCompiler(ICraftingGrid cc, World world, Set<IAEItemStack> boundaryKeys, boolean detectOnly,
            CycleAnalyzer.ProducerIndex producerIndex) {
        this.cc = cc;
        this.world = world;
        this.boundaryKeys = boundaryKeys;
        this.detectOnly = detectOnly;
        this.producerIndex = producerIndex;
    }

    /**
     * 两遍编译:第一遍 DFS 探环(回边目标记入边界集合,自身容错),
     * 第二遍把边界 key 当 CYCLE 叶子正式编译;出现边界外的新环 → 回落.
     */
    public static DagGraph compile(ICraftingGrid cc, IAEItemStack root, World world) throws DagFallback {
        try {
            IAEItemStack rootKey = RecursiveCraftingHelper.canon(root);
            Set<IAEItemStack> boundaryKeys = new HashSet<>();
            CycleAnalyzer.ProducerIndex producerIndex = new CycleAnalyzer.ProducerIndex(cc, world);
            try {
                new DagCompiler(cc, world, boundaryKeys, true, producerIndex).visit(rootKey);
            } catch (DagFallback ignored) {
                // 第一遍只负责发现边界;分支编译失败不影响(第二遍做真正的校验)
            }
            DagCompiler compiler = new DagCompiler(cc, world, boundaryKeys, false, producerIndex);
            DagGraph.DagNode rootNode = compiler.visit(rootKey);
            DagGraph graph = new DagGraph(rootNode);
            graph.hasMultiBranch = compiler.sawMultiBranch;
            // 逆后序:父节点(需求方)先于子节点(原料方)
            for (int i = compiler.postOrder.size() - 1; i >= 0; i--) {
                graph.topoOrder.add(compiler.postOrder.get(i));
            }
            return graph;
        } catch (StackOverflowError e) {
            throw new DagFallback("compile_stack_overflow");
        }
    }

    private DagGraph.DagNode visit(IAEItemStack key) throws DagFallback {
        DagGraph.DagNode existing = this.nodes.get(key);
        if (existing != null) {
            if (this.colors.get(key) == GRAY) {
                // 回边:有向环——记录/确认边界;第二遍中边界外的新环是编译缺陷
                if (this.detectOnly) {
                    this.boundaryKeys.add(key);
                    return existing;
                }
                throw new DagFallback("cycle_in_dag:" + key);
            }
            return existing; // BLACK:已编译,直接共享
        }
        if (this.nodes.size() >= maxNodes()) {
            throw new DagFallback("budget_nodes_exceeded");
        }
        if (!this.detectOnly && this.boundaryKeys.contains(key)) {
            // 循环边界:叶子节点,输入遍历委托 CycleBoundarySolver
            DagGraph.DagNode boundary = new DagGraph.DagNode(DagGraph.Kind.CYCLE, key, 0, null);
            this.nodes.put(key, boundary);
            this.colors.put(key, BLACK);
            this.postOrder.add(boundary);
            return boundary;
        }

        this.colors.put(key, GRAY);
        DagGraph.DagNode node = this.buildNode(key);
        this.nodes.put(key, node);
        if (node.kind == DagGraph.Kind.NORMAL) {
            this.visitInputs(node.pattern, node.edges);
            // 多样板接管:额外分支的输入同样展开(分支序 = 原生尝试序)
            for (DagGraph.Branch branch : node.extraBranches) {
                this.visitInputs(branch.pattern, branch.edges);
            }
        }
        this.colors.put(key, BLACK);
        this.postOrder.add(node);
        return node;
    }

    /** 展开一个分支的输入边(condensed 输入序 = 原生逐槽位处理序;携带替代候选). */
    private void visitInputs(ICraftingPatternDetails pattern, List<DagGraph.Edge> edges) throws DagFallback {
        for (IAEItemStack input : pattern.getCondensedInputs()) {
            if (input == null || input.getStackSize() <= 0) {
                continue;
            }
            long perCraft = input.getStackSize();
            edges.add(new DagGraph.Edge(this.visit(RecursiveCraftingHelper.canon(input)), perCraft,
                    substituteCandidates(pattern, input)));
        }
    }

    /**
     * 收集某 condensed 输入的替代候选(canon 键,不含编码输入自身;空 = 精确输入).
     * 仅可合成且启用替代标志的样板有候选;processing 样板不可调
     * getSubstituteInputs(standardRecipe == null 必 NPE),直接短路为空.
     * 同一物品多槽位时取各槽候选并集(同物品候选通常来自同一矿词条目).
     */
    private static List<IAEItemStack> substituteCandidates(ICraftingPatternDetails pattern,
            IAEItemStack encoded) {
        if (!pattern.isCraftable() || !pattern.canSubstitute()) {
            return java.util.Collections.emptyList();
        }
        Set<IAEItemStack> candidates = new LinkedHashSet<>();
        IAEItemStack[] inputs = pattern.getInputs();
        for (int slot = 0; slot < inputs.length; slot++) {
            IAEItemStack slotInput = inputs[slot];
            if (slotInput == null || !slotInput.isSameType(encoded)) {
                continue;
            }
            for (IAEItemStack substitute : pattern.getSubstituteInputs(slot)) {
                if (substitute != null && !slotInput.isSameType(substitute)) {
                    candidates.add(RecursiveCraftingHelper.canon(substitute));
                }
            }
        }
        return candidates.isEmpty() ? java.util.Collections.emptyList()
                : new ArrayList<>(candidates);
    }

    private DagGraph.DagNode buildNode(IAEItemStack key) throws DagFallback {
        // 对齐原生语义(CraftingTreeNode.addNode):可发射物品不展开任何样板分支——
        // 提取库存后剩余量由发射免费满足,即使有可用样板也不走合成
        if (this.cc.canEmitFor(key)) {
            return new DagGraph.DagNode(DagGraph.Kind.EMITTER, key, 0, null);
        }
        List<ICraftingPatternDetails> clean = new ArrayList<>();
        for (ICraftingPatternDetails pattern : this.cc.getCraftingFor(key, null, -1, this.world)) {
            // 替代候选不再是限制:边携带候选,执行器需求拆分提取(编码优先→候选交集),
            // 与原生"替代仅作用于库存提取、合成仍走编码物"语义一致
            clean.add(pattern);
        }
        if (clean.isEmpty()) {
            return new DagGraph.DagNode(DagGraph.Kind.TERMINAL, key, 0, null);
        }
        // 分层选样板:同优先级层内优先<b>非环</b>样板——嵌套环配方的计划会大幅
        // 膨胀(边界子树乘性展开),有可用的非环配方即避开循环边界;
        // 显式优先级层不被跨越:最高优先级层全部成环时才收缩为循环边界
        // (用户可通过提高环配方优先级强制走环).成环样板不再参与常规分支
        // (原 cycle_multi 整单回落随之消除).
        int topPriority = Integer.MIN_VALUE;
        for (ICraftingPatternDetails pattern : clean) {
            topPriority = Math.max(topPriority, pattern.getPriority());
        }
        DagGraph.DagNode node = null;
        for (ICraftingPatternDetails pattern : clean) {
            if (CycleAnalyzer.isCycleStep(this.cc, this.world, pattern, this.producerIndex)) {
                continue;
            }
            if (node == null) {
                if (pattern.getPriority() < topPriority) {
                    continue; // 顶层无非环:低层非环不启用(尊重显式优先级)
                }
                node = new DagGraph.DagNode(DagGraph.Kind.NORMAL, key, outPerOf(pattern, key),
                        pattern);
            } else {
                node.extraBranches.add(new DagGraph.Branch(pattern, outPerOf(pattern, key)));
            }
        }
        if (node == null) {
            // 最高优先级层全部成环 → 本节点收缩为循环边界,由 CycleBoundarySolver 联立求解
            return new DagGraph.DagNode(DagGraph.Kind.CYCLE, key, 0, null);
        }
        if (!node.extraBranches.isEmpty()) {
            this.sawMultiBranch = true;
        }
        return node;
    }

    /** 样板对本 key 的单次产出(累计全部输出槽);无产出即编译失败. */
    private static long outPerOf(ICraftingPatternDetails pattern, IAEItemStack key) throws DagFallback {
        long outPer = 0;
        for (IAEItemStack output : pattern.getCondensedOutputs()) {
            if (output != null && key.equals(output)) {
                outPer = SaturatedMath.add(outPer, output.getStackSize());
            }
        }
        if (outPer <= 0) {
            throw new DagFallback("pattern_without_output:" + key);
        }
        return outPer;
    }

    @Nullable
    public static String describe(@Nullable DagFallback fallback) {
        return fallback == null ? null : fallback.reason;
    }
}
