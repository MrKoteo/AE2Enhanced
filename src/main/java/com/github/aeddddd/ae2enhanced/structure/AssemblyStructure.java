package com.github.aeddddd.ae2enhanced.structure;

import com.github.aeddddd.ae2enhanced.ModBlocks;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class AssemblyStructure {

    // 坐标相对于几何中心 (0,0,0)
    public static final Set<BlockPos> CORE_SET;
    public static final Set<BlockPos> PART1_SET;
    public static final Set<BlockPos> PART2_SET;
    public static final Set<BlockPos> PART3_SET;
    public static final Set<BlockPos> PART4_SET;
    public static final Set<BlockPos> ALL_SET;

    static {
        Set<BlockPos> core = new HashSet<>();
        core.add(new BlockPos(0, 0, -7));
        CORE_SET = Collections.unmodifiableSet(core);

        Set<BlockPos> part1 = new HashSet<>();
        part1.add(new BlockPos(-7, 0, 0));
        part1.add(new BlockPos(7, 0, 0));
        part1.add(new BlockPos(0, 0, 7));
        PART1_SET = Collections.unmodifiableSet(part1);

        Set<BlockPos> part2 = new HashSet<>();
        part2.add(new BlockPos(-4, -1, -6));
        part2.add(new BlockPos(-4, -1, 6));
        part2.add(new BlockPos(-4, 0, -6));
        part2.add(new BlockPos(-4, 0, 6));
        part2.add(new BlockPos(-4, 1, -6));
        part2.add(new BlockPos(-4, 1, 6));
        part2.add(new BlockPos(-3, -1, -6));
        part2.add(new BlockPos(-3, -1, 6));
        part2.add(new BlockPos(-3, 0, -6));
        part2.add(new BlockPos(-3, 0, 6));
        part2.add(new BlockPos(-3, 1, -6));
        part2.add(new BlockPos(-3, 1, 6));
        part2.add(new BlockPos(-2, -1, -6));
        part2.add(new BlockPos(-2, -1, 6));
        part2.add(new BlockPos(-2, 0, -6));
        part2.add(new BlockPos(-2, 0, 6));
        part2.add(new BlockPos(-2, 1, -6));
        part2.add(new BlockPos(-2, 1, 6));
        part2.add(new BlockPos(2, -1, -6));
        part2.add(new BlockPos(2, -1, 6));
        part2.add(new BlockPos(2, 0, -6));
        part2.add(new BlockPos(2, 0, 6));
        part2.add(new BlockPos(2, 1, -6));
        part2.add(new BlockPos(2, 1, 6));
        part2.add(new BlockPos(3, -1, -6));
        part2.add(new BlockPos(3, -1, 6));
        part2.add(new BlockPos(3, 0, -6));
        part2.add(new BlockPos(3, 0, 6));
        part2.add(new BlockPos(3, 1, -6));
        part2.add(new BlockPos(3, 1, 6));
        part2.add(new BlockPos(4, -1, -6));
        part2.add(new BlockPos(4, -1, 6));
        part2.add(new BlockPos(4, 0, -6));
        part2.add(new BlockPos(4, 0, 6));
        part2.add(new BlockPos(4, 1, -6));
        part2.add(new BlockPos(4, 1, 6));
        part2.add(new BlockPos(-6, -1, -4));
        part2.add(new BlockPos(-6, -1, -3));
        part2.add(new BlockPos(-6, -1, -2));
        part2.add(new BlockPos(-6, -1, 2));
        part2.add(new BlockPos(-6, -1, 3));
        part2.add(new BlockPos(-6, -1, 4));
        part2.add(new BlockPos(-6, 0, -4));
        part2.add(new BlockPos(-6, 0, -3));
        part2.add(new BlockPos(-6, 0, -2));
        part2.add(new BlockPos(-6, 0, 2));
        part2.add(new BlockPos(-6, 0, 3));
        part2.add(new BlockPos(-6, 0, 4));
        part2.add(new BlockPos(-6, 1, -4));
        part2.add(new BlockPos(-6, 1, -3));
        part2.add(new BlockPos(-6, 1, -2));
        part2.add(new BlockPos(-6, 1, 2));
        part2.add(new BlockPos(-6, 1, 3));
        part2.add(new BlockPos(-6, 1, 4));
        part2.add(new BlockPos(6, -1, -4));
        part2.add(new BlockPos(6, -1, -3));
        part2.add(new BlockPos(6, -1, -2));
        part2.add(new BlockPos(6, -1, 2));
        part2.add(new BlockPos(6, -1, 3));
        part2.add(new BlockPos(6, -1, 4));
        part2.add(new BlockPos(6, 0, -4));
        part2.add(new BlockPos(6, 0, -3));
        part2.add(new BlockPos(6, 0, -2));
        part2.add(new BlockPos(6, 0, 2));
        part2.add(new BlockPos(6, 0, 3));
        part2.add(new BlockPos(6, 0, 4));
        part2.add(new BlockPos(6, 1, -4));
        part2.add(new BlockPos(6, 1, -3));
        part2.add(new BlockPos(6, 1, -2));
        part2.add(new BlockPos(6, 1, 2));
        part2.add(new BlockPos(6, 1, 3));
        part2.add(new BlockPos(6, 1, 4));
        part2.add(new BlockPos(-5, -1, -5));
        part2.add(new BlockPos(-5, -1, -4));
        part2.add(new BlockPos(-5, -1, 4));
        part2.add(new BlockPos(-5, -1, 5));
        part2.add(new BlockPos(-5, 0, -5));
        part2.add(new BlockPos(-5, 0, -4));
        part2.add(new BlockPos(-5, 0, 4));
        part2.add(new BlockPos(-5, 0, 5));
        part2.add(new BlockPos(-5, 1, -5));
        part2.add(new BlockPos(-5, 1, -4));
        part2.add(new BlockPos(-5, 1, 4));
        part2.add(new BlockPos(-5, 1, 5));
        part2.add(new BlockPos(5, -1, -5));
        part2.add(new BlockPos(5, -1, -4));
        part2.add(new BlockPos(5, -1, 4));
        part2.add(new BlockPos(5, -1, 5));
        part2.add(new BlockPos(5, 0, -5));
        part2.add(new BlockPos(5, 0, -4));
        part2.add(new BlockPos(5, 0, 4));
        part2.add(new BlockPos(5, 0, 5));
        part2.add(new BlockPos(5, 1, -5));
        part2.add(new BlockPos(5, 1, -4));
        part2.add(new BlockPos(5, 1, 4));
        part2.add(new BlockPos(5, 1, 5));
        part2.add(new BlockPos(-7, -1, -2));
        part2.add(new BlockPos(-7, -1, -1));
        part2.add(new BlockPos(-7, -1, 1));
        part2.add(new BlockPos(-7, -1, 2));
        part2.add(new BlockPos(-7, 0, -2));
        part2.add(new BlockPos(-7, 0, -1));
        part2.add(new BlockPos(-7, 0, 1));
        part2.add(new BlockPos(-7, 0, 2));
        part2.add(new BlockPos(-7, 1, -2));
        part2.add(new BlockPos(-7, 1, -1));
        part2.add(new BlockPos(-7, 1, 1));
        part2.add(new BlockPos(-7, 1, 2));
        part2.add(new BlockPos(7, -1, -2));
        part2.add(new BlockPos(7, -1, -1));
        part2.add(new BlockPos(7, -1, 1));
        part2.add(new BlockPos(7, -1, 2));
        part2.add(new BlockPos(7, 0, -2));
        part2.add(new BlockPos(7, 0, -1));
        part2.add(new BlockPos(7, 0, 1));
        part2.add(new BlockPos(7, 0, 2));
        part2.add(new BlockPos(7, 1, -2));
        part2.add(new BlockPos(7, 1, -1));
        part2.add(new BlockPos(7, 1, 1));
        part2.add(new BlockPos(7, 1, 2));
        part2.add(new BlockPos(-2, -1, -7));
        part2.add(new BlockPos(-2, -1, 7));
        part2.add(new BlockPos(-2, 1, -7));
        part2.add(new BlockPos(-2, 1, 7));
        part2.add(new BlockPos(-1, -1, -7));
        part2.add(new BlockPos(-1, -1, 7));
        part2.add(new BlockPos(-1, 1, -7));
        part2.add(new BlockPos(-1, 1, 7));
        part2.add(new BlockPos(0, -1, -7));
        part2.add(new BlockPos(0, -1, 7));
        part2.add(new BlockPos(0, 1, -7));
        part2.add(new BlockPos(0, 1, 7));
        part2.add(new BlockPos(1, -1, -7));
        part2.add(new BlockPos(1, -1, 7));
        part2.add(new BlockPos(1, 1, -7));
        part2.add(new BlockPos(1, 1, 7));
        part2.add(new BlockPos(2, -1, -7));
        part2.add(new BlockPos(2, -1, 7));
        part2.add(new BlockPos(2, 1, -7));
        part2.add(new BlockPos(2, 1, 7));
        part2.add(new BlockPos(-4, -1, -5));
        part2.add(new BlockPos(-4, -1, 5));
        part2.add(new BlockPos(-4, 0, -5));
        part2.add(new BlockPos(-4, 0, 5));
        part2.add(new BlockPos(-4, 1, -5));
        part2.add(new BlockPos(-4, 1, 5));
        part2.add(new BlockPos(4, -1, -5));
        part2.add(new BlockPos(4, -1, 5));
        part2.add(new BlockPos(4, 0, -5));
        part2.add(new BlockPos(4, 0, 5));
        part2.add(new BlockPos(4, 1, -5));
        part2.add(new BlockPos(4, 1, 5));
        part2.add(new BlockPos(-3, 0, -7));
        part2.add(new BlockPos(-3, 0, 7));
        part2.add(new BlockPos(-2, 0, -7));
        part2.add(new BlockPos(-2, 0, 7));
        part2.add(new BlockPos(-1, 0, -7));
        part2.add(new BlockPos(-1, 0, 7));
        part2.add(new BlockPos(1, 0, -7));
        part2.add(new BlockPos(1, 0, 7));
        part2.add(new BlockPos(2, 0, -7));
        part2.add(new BlockPos(2, 0, 7));
        part2.add(new BlockPos(3, 0, -7));
        part2.add(new BlockPos(3, 0, 7));
        part2.add(new BlockPos(-5, 0, -6));
        part2.add(new BlockPos(-5, 0, 6));
        part2.add(new BlockPos(5, 0, -6));
        part2.add(new BlockPos(5, 0, 6));
        part2.add(new BlockPos(-6, 0, -5));
        part2.add(new BlockPos(-6, 0, 5));
        part2.add(new BlockPos(6, 0, -5));
        part2.add(new BlockPos(6, 0, 5));
        part2.add(new BlockPos(-7, -1, 0));
        part2.add(new BlockPos(-7, 1, 0));
        part2.add(new BlockPos(7, -1, 0));
        part2.add(new BlockPos(7, 1, 0));
        part2.add(new BlockPos(-7, 0, -3));
        part2.add(new BlockPos(-7, 0, 3));
        part2.add(new BlockPos(7, 0, -3));
        part2.add(new BlockPos(7, 0, 3));
        PART2_SET = Collections.unmodifiableSet(part2);

        Set<BlockPos> part3 = new HashSet<>();
        part3.add(new BlockPos(-3, -2, -5));
        part3.add(new BlockPos(-3, -2, 5));
        part3.add(new BlockPos(-3, -1, -5));
        part3.add(new BlockPos(-3, -1, 5));
        part3.add(new BlockPos(-3, 1, -5));
        part3.add(new BlockPos(-3, 1, 5));
        part3.add(new BlockPos(-3, 2, -5));
        part3.add(new BlockPos(-3, 2, 5));
        part3.add(new BlockPos(-2, -2, -5));
        part3.add(new BlockPos(-2, -2, 5));
        part3.add(new BlockPos(-2, -1, -5));
        part3.add(new BlockPos(-2, -1, 5));
        part3.add(new BlockPos(-2, 1, -5));
        part3.add(new BlockPos(-2, 1, 5));
        part3.add(new BlockPos(-2, 2, -5));
        part3.add(new BlockPos(-2, 2, 5));
        part3.add(new BlockPos(2, -2, -5));
        part3.add(new BlockPos(2, -2, 5));
        part3.add(new BlockPos(2, -1, -5));
        part3.add(new BlockPos(2, -1, 5));
        part3.add(new BlockPos(2, 1, -5));
        part3.add(new BlockPos(2, 1, 5));
        part3.add(new BlockPos(2, 2, -5));
        part3.add(new BlockPos(2, 2, 5));
        part3.add(new BlockPos(3, -2, -5));
        part3.add(new BlockPos(3, -2, 5));
        part3.add(new BlockPos(3, -1, -5));
        part3.add(new BlockPos(3, -1, 5));
        part3.add(new BlockPos(3, 1, -5));
        part3.add(new BlockPos(3, 1, 5));
        part3.add(new BlockPos(3, 2, -5));
        part3.add(new BlockPos(3, 2, 5));
        part3.add(new BlockPos(-5, -2, -3));
        part3.add(new BlockPos(-5, -2, -2));
        part3.add(new BlockPos(-5, -2, 2));
        part3.add(new BlockPos(-5, -2, 3));
        part3.add(new BlockPos(-5, -1, -3));
        part3.add(new BlockPos(-5, -1, -2));
        part3.add(new BlockPos(-5, -1, 2));
        part3.add(new BlockPos(-5, -1, 3));
        part3.add(new BlockPos(-5, 1, -3));
        part3.add(new BlockPos(-5, 1, -2));
        part3.add(new BlockPos(-5, 1, 2));
        part3.add(new BlockPos(-5, 1, 3));
        part3.add(new BlockPos(-5, 2, -3));
        part3.add(new BlockPos(-5, 2, -2));
        part3.add(new BlockPos(-5, 2, 2));
        part3.add(new BlockPos(-5, 2, 3));
        part3.add(new BlockPos(5, -2, -3));
        part3.add(new BlockPos(5, -2, -2));
        part3.add(new BlockPos(5, -2, 2));
        part3.add(new BlockPos(5, -2, 3));
        part3.add(new BlockPos(5, -1, -3));
        part3.add(new BlockPos(5, -1, -2));
        part3.add(new BlockPos(5, -1, 2));
        part3.add(new BlockPos(5, -1, 3));
        part3.add(new BlockPos(5, 1, -3));
        part3.add(new BlockPos(5, 1, -2));
        part3.add(new BlockPos(5, 1, 2));
        part3.add(new BlockPos(5, 1, 3));
        part3.add(new BlockPos(5, 2, -3));
        part3.add(new BlockPos(5, 2, -2));
        part3.add(new BlockPos(5, 2, 2));
        part3.add(new BlockPos(5, 2, 3));
        part3.add(new BlockPos(-1, -2, -6));
        part3.add(new BlockPos(-1, -2, 6));
        part3.add(new BlockPos(-1, -1, -6));
        part3.add(new BlockPos(-1, -1, 6));
        part3.add(new BlockPos(-1, 1, -6));
        part3.add(new BlockPos(-1, 1, 6));
        part3.add(new BlockPos(-1, 2, -6));
        part3.add(new BlockPos(-1, 2, 6));
        part3.add(new BlockPos(0, -2, -6));
        part3.add(new BlockPos(0, -2, 6));
        part3.add(new BlockPos(0, -1, -6));
        part3.add(new BlockPos(0, -1, 6));
        part3.add(new BlockPos(0, 1, -6));
        part3.add(new BlockPos(0, 1, 6));
        part3.add(new BlockPos(0, 2, -6));
        part3.add(new BlockPos(0, 2, 6));
        part3.add(new BlockPos(1, -2, -6));
        part3.add(new BlockPos(1, -2, 6));
        part3.add(new BlockPos(1, -1, -6));
        part3.add(new BlockPos(1, -1, 6));
        part3.add(new BlockPos(1, 1, -6));
        part3.add(new BlockPos(1, 1, 6));
        part3.add(new BlockPos(1, 2, -6));
        part3.add(new BlockPos(1, 2, 6));
        part3.add(new BlockPos(-6, -2, -1));
        part3.add(new BlockPos(-6, -2, 0));
        part3.add(new BlockPos(-6, -2, 1));
        part3.add(new BlockPos(-6, -1, -1));
        part3.add(new BlockPos(-6, -1, 0));
        part3.add(new BlockPos(-6, -1, 1));
        part3.add(new BlockPos(-6, 1, -1));
        part3.add(new BlockPos(-6, 1, 0));
        part3.add(new BlockPos(-6, 1, 1));
        part3.add(new BlockPos(-6, 2, -1));
        part3.add(new BlockPos(-6, 2, 0));
        part3.add(new BlockPos(-6, 2, 1));
        part3.add(new BlockPos(6, -2, -1));
        part3.add(new BlockPos(6, -2, 0));
        part3.add(new BlockPos(6, -2, 1));
        part3.add(new BlockPos(6, -1, -1));
        part3.add(new BlockPos(6, -1, 0));
        part3.add(new BlockPos(6, -1, 1));
        part3.add(new BlockPos(6, 1, -1));
        part3.add(new BlockPos(6, 1, 0));
        part3.add(new BlockPos(6, 1, 1));
        part3.add(new BlockPos(6, 2, -1));
        part3.add(new BlockPos(6, 2, 0));
        part3.add(new BlockPos(6, 2, 1));
        part3.add(new BlockPos(-4, -2, -4));
        part3.add(new BlockPos(-4, -2, 4));
        part3.add(new BlockPos(-4, -1, -4));
        part3.add(new BlockPos(-4, -1, 4));
        part3.add(new BlockPos(-4, 1, -4));
        part3.add(new BlockPos(-4, 1, 4));
        part3.add(new BlockPos(-4, 2, -4));
        part3.add(new BlockPos(-4, 2, 4));
        part3.add(new BlockPos(4, -2, -4));
        part3.add(new BlockPos(4, -2, 4));
        part3.add(new BlockPos(4, -1, -4));
        part3.add(new BlockPos(4, -1, 4));
        part3.add(new BlockPos(4, 1, -4));
        part3.add(new BlockPos(4, 1, 4));
        part3.add(new BlockPos(4, 2, -4));
        part3.add(new BlockPos(4, 2, 4));
        PART3_SET = Collections.unmodifiableSet(part3);

        Set<BlockPos> part4 = new HashSet<>();
        part4.add(new BlockPos(-5, 0, -3));
        part4.add(new BlockPos(-5, 0, -2));
        part4.add(new BlockPos(-5, 0, 2));
        part4.add(new BlockPos(-5, 0, 3));
        part4.add(new BlockPos(5, 0, -3));
        part4.add(new BlockPos(5, 0, -2));
        part4.add(new BlockPos(5, 0, 2));
        part4.add(new BlockPos(5, 0, 3));
        part4.add(new BlockPos(-3, 0, -5));
        part4.add(new BlockPos(-3, 0, 5));
        part4.add(new BlockPos(-2, 0, -5));
        part4.add(new BlockPos(-2, 0, 5));
        part4.add(new BlockPos(2, 0, -5));
        part4.add(new BlockPos(2, 0, 5));
        part4.add(new BlockPos(3, 0, -5));
        part4.add(new BlockPos(3, 0, 5));
        part4.add(new BlockPos(-1, 0, -6));
        part4.add(new BlockPos(-1, 0, 6));
        part4.add(new BlockPos(0, 0, -6));
        part4.add(new BlockPos(0, 0, 6));
        part4.add(new BlockPos(1, 0, -6));
        part4.add(new BlockPos(1, 0, 6));
        part4.add(new BlockPos(-6, 0, -1));
        part4.add(new BlockPos(-6, 0, 0));
        part4.add(new BlockPos(-6, 0, 1));
        part4.add(new BlockPos(6, 0, -1));
        part4.add(new BlockPos(6, 0, 0));
        part4.add(new BlockPos(6, 0, 1));
        part4.add(new BlockPos(-4, 0, -4));
        part4.add(new BlockPos(-4, 0, 4));
        part4.add(new BlockPos(4, 0, -4));
        part4.add(new BlockPos(4, 0, 4));
        PART4_SET = Collections.unmodifiableSet(part4);

        Set<BlockPos> all = new HashSet<>();
        all.addAll(CORE_SET);
        all.addAll(PART1_SET);
        all.addAll(PART2_SET);
        all.addAll(PART3_SET);
        all.addAll(PART4_SET);
        ALL_SET = Collections.unmodifiableSet(all);
    }

    /**
     * 由控制器位置获取几何中心原点
     */
    public static BlockPos getOriginFromController(BlockPos controllerPos) {
        return controllerPos.add(0, 0, 7);
    }

    /**
     * 验证结构完整性（优先检查 core 与 part1）
     */
    public static boolean validate(World world, BlockPos controllerPos) {
        BlockPos origin = getOriginFromController(controllerPos);

        // 优先检查 core
        if (!checkBlock(world, origin, CORE_SET, ModBlocks.ASSEMBLY_CONTROLLER)) {
            return false;
        }

        // 优先检查 part1（ME 接口，结构完整性关键）
        if (!checkBlock(world, origin, PART1_SET, ModBlocks.ASSEMBLY_ME_INTERFACE)) {
            return false;
        }

        if (!checkBlock(world, origin, PART2_SET, ModBlocks.ASSEMBLY_CASING)) {
            return false;
        }
        if (!checkBlock(world, origin, PART3_SET, ModBlocks.ASSEMBLY_INNER_WALL)) {
            return false;
        }
        if (!checkBlock(world, origin, PART4_SET, ModBlocks.ASSEMBLY_STABILIZER)) {
            return false;
        }

        return true;
    }

    private static boolean checkBlock(World world, BlockPos origin, Set<BlockPos> relativeSet, Block expected) {
        for (BlockPos rel : relativeSet) {
            BlockPos actual = origin.add(rel);
            if (world.getBlockState(actual).getBlock() != expected) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取缺失方块清单（用于未组装 GUI 显示）
     */
    public static Map<Block, Integer> getMissingMap(World world, BlockPos controllerPos) {
        BlockPos origin = getOriginFromController(controllerPos);
        Map<Block, Integer> missing = new LinkedHashMap<>();

        countMissing(world, origin, PART1_SET, ModBlocks.ASSEMBLY_ME_INTERFACE, missing);
        countMissing(world, origin, PART2_SET, ModBlocks.ASSEMBLY_CASING, missing);
        countMissing(world, origin, PART3_SET, ModBlocks.ASSEMBLY_INNER_WALL, missing);
        countMissing(world, origin, PART4_SET, ModBlocks.ASSEMBLY_STABILIZER, missing);

        return missing;
    }

    private static void countMissing(World world, BlockPos origin, Set<BlockPos> relativeSet, Block expected, Map<Block, Integer> missing) {
        for (BlockPos rel : relativeSet) {
            BlockPos actual = origin.add(rel);
            if (world.getBlockState(actual).getBlock() != expected) {
                missing.put(expected, missing.getOrDefault(expected, 0) + 1);
            }
        }
    }

    /**
     * 组装：通知 TileEntity 进入已组装状态
     */
    public static void assemble(World world, BlockPos controllerPos) {
        if (world.isRemote) return;
        TileAssemblyController tile = getControllerTile(world, controllerPos);
        if (tile != null) {
            tile.assemble();
        }
    }

    /**
     * 解散：通知 TileEntity 进入未组装状态
     */
    public static void disassemble(World world, BlockPos controllerPos) {
        if (world.isRemote) return;
        TileAssemblyController tile = getControllerTile(world, controllerPos);
        if (tile != null) {
            tile.disassemble();
        }
    }

    private static TileAssemblyController getControllerTile(World world, BlockPos pos) {
        // 注意：控制器在 origin + (0,0,-7)，也就是 controllerPos 参数本身就是控制器位置
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        return te instanceof TileAssemblyController ? (TileAssemblyController) te : null;
    }
}
