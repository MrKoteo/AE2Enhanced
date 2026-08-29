package com.github.aeddddd.ae2enhanced.diag.check;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.hooks.TickHandler;
import appeng.me.Grid;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * AE2 网格健康检查.
 *
 * <p>遍历服务端全部网格（与 migratefluids 相同的 TickHandler 入口）：</p>
 * <ul>
 *   <li>空网格 → WARN</li>
 *   <li>控制器冲突 → ERROR（NO_CONTROLLER 为合法自组网,不告警）</li>
 *   <li>有功耗但储能为零 → WARN（可能断电）</li>
 *   <li>汇总：网格数 / 节点数 / 合成 CPU 忙碌数 / 总功耗</li>
 * </ul>
 */
public final class GridCheck implements SystemCheck {

    @Override
    public String name() {
        return "grid";
    }

    @Override
    public String displayName() {
        return "AE2 网格";
    }

    @Override
    public void run(MinecraftServer server, List<CheckResult> out) {
        try {
            int grids = 0;
            int nodes = 0;
            int emptyGrids = 0;
            int conflicted = 0;
            int unpowered = 0;
            int cpusTotal = 0;
            int cpusBusy = 0;
            double totalPowerUsage = 0.0;

            for (Grid grid : TickHandler.INSTANCE.getGridList()) {
                grids++;
                if (grid.isEmpty()) {
                    emptyGrids++;
                    continue;
                }
                nodes += grid.getNodes().size();

                IPathingGrid pathing = grid.getCache(IPathingGrid.class);
                if (pathing.getControllerState() == ControllerState.CONTROLLER_CONFLICT) {
                    conflicted++;
                }

                IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
                totalPowerUsage += energy.getAvgPowerUsage();
                if (energy.getAvgPowerUsage() > 0.0 && energy.getStoredPower() <= 0.0) {
                    unpowered++;
                }

                ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
                for (ICraftingCPU cpu : crafting.getCpus()) {
                    cpusTotal++;
                    if (cpu.isBusy()) {
                        cpusBusy++;
                    }
                }
            }

            out.add(CheckResult.ok("网格: " + grids + " 个,节点: " + nodes + " 个,平均功耗: "
                    + String.format(java.util.Locale.ROOT, "%.1f", totalPowerUsage) + " AE/t"));
            out.add(CheckResult.ok("合成 CPU: " + cpusTotal + " 个(忙碌 " + cpusBusy + " 个)"));
            if (emptyGrids > 0) {
                out.add(CheckResult.warn("空网格: " + emptyGrids + " 个"));
            }
            if (conflicted > 0) {
                out.add(CheckResult.error("控制器冲突网格: " + conflicted + " 个"));
            }
            if (unpowered > 0) {
                out.add(CheckResult.warn("疑似断电网格(有功耗但储能为零): " + unpowered + " 个"));
            }
        } catch (Exception e) {
            out.add(CheckResult.error("网格检查执行异常: " + e));
        }
    }
}
