package com.github.aeddddd.ae2enhanced.api;

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
import com.github.aeddddd.ae2enhanced.integration.botaniaapplie.BotaniaApplieCompat;
import com.github.aeddddd.ae2enhanced.storage.external.ExternalStackFactory;
import com.github.aeddddd.ae2enhanced.storage.mana.ManaChannelResolver;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 网络 Mana（Botania 魔力）对外 API.
 *
 * <p>允许外部模组（如 MMCE-addition）以 {@link IActionHost} 的身份直接查询/消耗
 * 当前 AE 网络中存储的 Mana（魔力存储通道，1 stackSize = 1 Mana），
 * 无需经过任何魔力仓方块。</p>
 *
 * <p>本类位于稳定的 {@code api} 包中，方法签名保持向后兼容；
 * 外部模组可通过反射调用以保持软依赖：
 * {@code Class.forName("com.github.aeddddd.ae2enhanced.api.NetworkManaApi")}.</p>
 *
 * <p>通道经 {@link ManaChannelResolver} 解析：Botania_Applie 外部通道在场时优先
 * 使用外部通道，此时堆叠创建必须走 NBT 路径（键 {@code "mana"}），
 * 自有通道使用键 {@code "Count"}（与统一资源终端的处理一致）。</p>
 */
public final class NetworkManaApi {

    private NetworkManaApi() {
    }

    /**
     * 查询宿主所在网络当前存储的 Mana 总量.
     *
     * @param host 已接入网络的 AE 设备宿主
     * @return 网络中可提取的 Mana 总量；未接入网络或查询失败时返回 0
     */
    public static long getStoredMana(IActionHost host) {
        return extractMana(host, Long.MAX_VALUE, true);
    }

    /**
     * 从宿主所在网络提取 Mana.
     *
     * <p>与 AE2 存储语义一致，网络存量不足时可能发生部分提取，
     * 返回值为实际提取到的数量。需要"全有或全无"语义的调用方应先以
     * {@code simulate=true} 确认存量充足，再实际提取。</p>
     *
     * @param host     已接入网络的 AE 设备宿主
     * @param amount   要提取的 Mana 数量（必须 &gt; 0）
     * @param simulate true = 仅模拟，不实际扣除
     * @return 实际（或模拟可）提取到的 Mana 数量
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static long extractMana(IActionHost host, long amount, boolean simulate) {
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
            IStorageChannel channel = ManaChannelResolver.getChannel();
            if (channel == null) {
                return 0;
            }
            IMEInventory inventory = storage.getInventory(channel);
            if (inventory == null) {
                return 0;
            }
            IAEStack request = createChannelStack(channel, amount);
            if (request == null) {
                return 0;
            }
            IActionSource source = new MachineSource(host);
            IAEStack extracted = (IAEStack) inventory.extractItems(request,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return extracted == null ? 0 : extracted.getStackSize();
        } catch (Exception e) {
            // 节点未就绪（GridAccessException）或通道未注册等情况：静默视为无 Mana
            return 0;
        }
    }

    /**
     * 按当前生效通道创建 Mana 堆叠.
     * 外部通道（Botania_Applie）NBT 键为 {@code "mana"}，自有通道为 {@code "Count"}，
     * 与 {@code UnifiedResourceTerminalServer} 的处理保持一致。
     */
    @SuppressWarnings("rawtypes")
    private static IAEStack createChannelStack(IStorageChannel channel, long amount) {
        String nbtKey = BotaniaApplieCompat.isManaStorageChannelAvailable() ? "mana" : "Count";
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong(nbtKey, amount);
        IAEStack stack = ExternalStackFactory.createFromNBT(channel, nbt);
        if (stack != null) {
            stack.setStackSize(amount);
        }
        return stack;
    }
}
