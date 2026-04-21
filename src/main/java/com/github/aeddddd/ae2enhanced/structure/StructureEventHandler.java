package com.github.aeddddd.ae2enhanced.structure;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.ModBlocks;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public class StructureEventHandler {

    // 待验证的控制器位置 -> 剩余 tick
    private static final Map<BlockPos, Integer> pendingChecks = new HashMap<>();

    @SubscribeEvent
    public static void onNeighborNotify(net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        checkSurroundingControllers(world, pos);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        checkSurroundingControllers(world, pos);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

        World world = event.world;
        pendingChecks.entrySet().removeIf(entry -> {
            BlockPos controllerPos = entry.getKey();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                validateAndUpdate(world, controllerPos);
                return true;
            }
            entry.setValue(ticks);
            return false;
        });
    }

    private static void checkSurroundingControllers(World world, BlockPos changedPos) {
        // 搜索以 changedPos 为中心、半径约 15 格范围内的控制器
        // 为了效率，改为检查 changedPos 是否属于某个已知结构的 ALL_SET 偏移
        // 简化：遍历附近可能的几何中心，反推控制器位置
        // 需要修改优化！！
        for (int dx = -15; dx <= 15; dx++) {
            for (int dy = -15; dy <= 15; dy++) {
                for (int dz = -15; dz <= 15; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 15) continue;
                    BlockPos checkPos = changedPos.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).getBlock() == ModBlocks.ASSEMBLY_CONTROLLER) {
                        scheduleCheck(checkPos);
                    }
                }
            }
        }
    }

    private static void scheduleCheck(BlockPos controllerPos) {
        pendingChecks.put(controllerPos, 20);
    }

    private static void validateAndUpdate(World world, BlockPos controllerPos) {
        if (world.getBlockState(controllerPos).getBlock() != ModBlocks.ASSEMBLY_CONTROLLER) {
            return;
        }

        boolean valid = AssemblyStructure.validate(world, controllerPos);
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof TileAssemblyController) {
            TileAssemblyController tile = (TileAssemblyController) te;
            if (valid && !tile.isFormed()) {
                AssemblyStructure.assemble(world, controllerPos);
            } else if (!valid && tile.isFormed()) {
                AssemblyStructure.disassemble(world, controllerPos);
            }
        }
    }
}
