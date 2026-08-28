package com.github.aeddddd.ae2enhanced.api;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.helpers.MachineSource;
import com.github.aeddddd.ae2enhanced.storage.starlight.AEStarlightStack;
import com.github.aeddddd.ae2enhanced.storage.starlight.IStarlightStorageChannel;

/**
 * 网络 Starlight（Astral Sorcery 星能）对外 API.
 *
 * <p>允许外部模组（如 MMCE-addition）以 {@link IActionHost} 的身份直接查询/消耗
 * 当前 AE 网络中存储的星能（星能存储通道，1 stackSize = 1 Starlight 单位），
 * 无需经过任何星能仓方块。</p>
 *
 * <p>本类位于稳定的 {@code api} 包中，方法签名保持向后兼容；
 * 外部模组可通过反射调用以保持软依赖：
 * {@code Class.forName("com.github.aeddddd.ae2enhanced.api.NetworkStarlightApi")}.</p>
 *
 * <p>星能通道无外部通道兜底，直接使用 AE2E 自有 {@link IStarlightStorageChannel}
 * （仅在 Astral Sorcery 在场时注册，未注册时所有操作返回 0）。</p>
 */
public final class NetworkStarlightApi {

    private NetworkStarlightApi() {
    }

    /**
     * 查询宿主所在网络当前存储的星能总量.
     *
     * @param host 已接入网络的 AE 设备宿主
     * @return 网络中可提取的星能总量；未接入网络或查询失败时返回 0
     */
    public static long getStoredStarlight(IActionHost host) {
        return extractStarlight(host, Long.MAX_VALUE, true);
    }

    /**
     * 从宿主所在网络提取星能.
     *
     * <p>与 AE2 存储语义一致，网络存量不足时可能发生部分提取，
     * 返回值为实际提取到的数量。需要"全有或全无"语义的调用方应先以
     * {@code simulate=true} 确认存量充足，再实际提取。</p>
     *
     * @param host     已接入网络的 AE 设备宿主
     * @param amount   要提取的星能数量（必须 &gt; 0）
     * @param simulate true = 仅模拟，不实际扣除
     * @return 实际（或模拟可）提取到的星能数量
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static long extractStarlight(IActionHost host, long amount, boolean simulate) {
        if (host == null || amount <= 0) {
            return 0;
        }
        try {
            IGridNode node = host.getActionableNode();
            if (node == null) {
                return 0;
            }
            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage == null) {
                return 0;
            }
            IStorageChannel channel = AEApi.instance().storage()
                    .getStorageChannel(IStarlightStorageChannel.class);
            if (channel == null) {
                return 0;
            }
            IMEInventory inventory = storage.getInventory(channel);
            if (inventory == null) {
                return 0;
            }
            IAEStack request = AEStarlightStack.create(amount);
            if (request == null) {
                return 0;
            }
            IActionSource source = new MachineSource(host);
            IAEStack extracted = (IAEStack) inventory.extractItems(request,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return extracted == null ? 0 : extracted.getStackSize();
        } catch (Exception e) {
            // 节点未就绪（GridAccessException）或通道未注册等情况：静默视为无星能
            return 0;
        }
    }
}
