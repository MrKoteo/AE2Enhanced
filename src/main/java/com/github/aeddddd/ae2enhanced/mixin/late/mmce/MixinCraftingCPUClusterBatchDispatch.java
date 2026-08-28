package com.github.aeddddd.ae2enhanced.mixin.late.mmce;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.integration.mmce.MMCEAdditionReflect;
import com.github.aeddddd.ae2enhanced.integration.randomcomplement.RCIntelligentBlockingReflect;
import com.github.aeddddd.ae2enhanced.mixin.late.accessor.ITaskProgressAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.CapabilityItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MMCE 仓室批量发配：一次 pushPattern 结算 N 份配方.
 *
 * <p>背景：AE2 CPU 每次 pushPattern 只派发 1 份样板，发配总次数 = 订单量/单份产出，
 * 极大下单时发配开销（材料提取 + 全槽模拟/真实插入 + 事件）随订单量线性爆炸。
 * MMCE 的样板仓室内部为计数式缓存（int 动态列表 / Long map），pushPattern 不校验
 * table 份数、直接全量追加，天然支持放大后的 table 一次塞入 N 份。</p>
 *
 * <p>适用目标（仅处理样板 {@code !details.isCraftable()}）：</p>
 * <ul>
 *   <li>MMCE {@link MEPatternProvider}（机械样板供应器）：
 *       仅 DEFAULT / ISOLATION_INPUT 工作模式（阻挡、强化阻挡、合成锁定语义上都
 *       意图限制发配，全部排除）；int 计数缓存，数量上限由 int 安全检查兜底。</li>
 *   <li>MMCE-addition 样板总成（TileMEPatternAssembly）：Long 缓冲恒可叠加
 *       （isBusy 恒 false），直接批量。</li>
 *   <li>ME 接口/二合一接口（{@link IInterfaceHost}，含 ae2fc 双接口）：
 *       非阻挡模式（或阻挡 + RandomComplement 智能阻挡同时开启），且所有已加载
 *       目标面均为 MMCE-addition 输入总成仓时批量
 *       （混接普通库存时保持原生逐份行为，避免放大后 acceptsItems 失败空转）。</li>
 * </ul>
 *
 * <p>批量语义：批量大小 N = min(任务剩余数, int 安全上限, 材料可用量)。
 * 额外的 (N-1) 份材料从 CPU 库存（下单时已预提整单材料）提取并随放大后的 table
 * 一次性派发；pushPattern 失败时额外材料回滚注入 CPU 库存。
 * 成功后按原生逐份记账的口径补记 (N-1) 份：taskProgress / remainingItemCount /
 * waitingFor / postChange / postCraftingStatusChange（与 VirtualBatch 契约一致，
 * 整批计 1 次 remainingOperations）。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterBatchDispatch {

    @Unique
    private static final boolean AE2E_CRAZYAE_LOADED = Loader.isModLoaded("crazyae");

    @Shadow
    private Map<ICraftingPatternDetails, Object> tasks;

    @Shadow
    private long remainingItemCount;

    @Shadow
    private IItemList<IAEItemStack> waitingFor;

    @Shadow
    private MachineSource machineSrc;

    @Shadow
    public abstract IMEInventory<IAEItemStack> getInventory();

    @Shadow
    private void postChange(IAEItemStack diff, appeng.api.networking.security.IActionSource src) {
    }

    @Shadow
    private void postCraftingStatusChange(IAEItemStack diff) {
    }

    @WrapOperation(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"))
    private boolean ae2e$batchDispatch(ICraftingMedium medium, ICraftingPatternDetails details,
            InventoryCrafting table, Operation<Boolean> original) {
        long batch = 1;
        if (!AE2E_CRAZYAE_LOADED && !details.isCraftable()) {
            try {
                if (ae2e$isBatchTarget(medium)) {
                    batch = ae2e$computeBatch(details, table);
                }
            } catch (Throwable t) {
                AE2Enhanced.LOGGER.error("[AE2E] MMCE batch dispatch check failed: {}", t.toString(), t);
                batch = 1;
            }
        }
        if (batch <= 1) {
            return original.call(medium, details, table);
        }

        IMEInventory<IAEItemStack> inv = this.getInventory();
        // 提取额外 (batch-1) 份材料（下单时已预提整单到 CPU 库存，正常必然满足）
        List<IAEItemStack> extras = new ArrayList<>();
        boolean extractedAll = true;
        for (IAEItemStack input : details.getInputs()) {
            if (input == null || input.getStackSize() <= 0) {
                continue;
            }
            IAEItemStack need = input.copy();
            need.setStackSize(input.getStackSize() * (batch - 1));
            IAEItemStack got = inv.extractItems(need, Actionable.MODULATE, this.machineSrc);
            long gotSize = got == null ? 0 : got.getStackSize();
            if (gotSize > 0) {
                extras.add(got);
                this.postChange(got.copy(), this.machineSrc);
            }
            if (gotSize < need.getStackSize()) {
                extractedAll = false;
                break;
            }
        }
        if (!extractedAll) {
            // 材料不足（不应发生，SIMULATE 已核算）：回滚并退化为原生逐份
            for (IAEItemStack e : extras) {
                inv.injectItems(e, Actionable.MODULATE, this.machineSrc);
            }
            return original.call(medium, details, table);
        }

        boolean result = original.call(medium, details, ae2e$scaleTable(table, batch));
        if (!result) {
            for (IAEItemStack e : extras) {
                inv.injectItems(e, Actionable.MODULATE, this.machineSrc);
            }
            return false;
        }

        ae2e$correctCounts(details, batch - 1);
        return true;
    }

    /**
     * 判定 medium 是否为可批量发配的 MMCE 目标.
     */
    @Unique
    private boolean ae2e$isBatchTarget(ICraftingMedium medium) {
        if (medium instanceof MEPatternProvider) {
            // 用 name() 比较而非枚举常量：编译期依赖的 MMCE-CE 2.2.2 尚无 ISOLATION_INPUT，
            // 直接引用新常量会导致编译失败；字符串比较对任意版本均安全。
            String wm = ((MEPatternProvider) medium).getWorkMode().name();
            return "DEFAULT".equals(wm) || "ISOLATION_INPUT".equals(wm);
        }
        if (MMCEAdditionReflect.isPatternAssembly(medium)) {
            return true;
        }
        if (medium instanceof IInterfaceHost) {
            // mmceaddition 未安装时不存在输入总成，跳过目标面扫描（普通接口热路径零开销）
            if (!MMCEAdditionReflect.isAvailable()) {
                return false;
            }
            IInterfaceHost host = (IInterfaceHost) medium;
            DualityInterface duality = host.getInterfaceDuality();
            if (duality == null) {
                return false;
            }
            // 阻挡模式：仅当同时开启 RandomComplement 智能阻挡时允许批量
            // （智能阻挡语义 = 相同样板在目标有物时仍可继续推送，与同样板批量 N 份等价）
            if (duality.getConfigManager().getSetting(Settings.BLOCK) == YesNo.YES
                    && !RCIntelligentBlockingReflect.isIntelligentBlockingOpen(duality)) {
                return false;
            }
            TileEntity tile = host.getTileEntity();
            World world = tile.getWorld();
            if (world == null) {
                return false;
            }
            boolean anyInputAssembly = false;
            for (EnumFacing face : host.getTargets()) {
                TileEntity te = world.getTileEntity(tile.getPos().offset(face));
                if (te == null) {
                    continue; // 未加载的面在 pushPattern 中会被跳过，不视为混接
                }
                if (MMCEAdditionReflect.isInputAssembly(te)) {
                    anyInputAssembly = true;
                    continue;
                }
                // 只对能实际接收发配的面做混接判定：线缆等无接收能力的 TE 在
                // pushPattern 中本就因 getAdaptor == null 被跳过，不应阻断批量
                if (ae2e$canFaceReceive(te, face)) {
                    return false; // 混接普通库存/机器 → 保持原生逐份
                }
            }
            return anyInputAssembly;
        }
        return false;
    }

    /**
     * 判定该面的 TE 是否能实际接收接口发配（与 DualityInterface.pushPattern 的
     * 三个分支对齐：接口对接口 / ICraftingMachine / InventoryAdaptor）.
     */
    @Unique
    private static boolean ae2e$canFaceReceive(TileEntity te, EnumFacing face) {
        if (te instanceof IInterfaceHost || te instanceof ICraftingMachine) {
            return true;
        }
        EnumFacing opposite = face.getOpposite();
        if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, opposite)) {
            return true;
        }
        // ae2fc 的 FluidConvertingInventoryAdaptor 使纯流体 capability 也能接收流体 packet
        if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, opposite)) {
            return true;
        }
        return te instanceof IInventory;
    }

    /**
     * 计算批量大小：min(任务剩余数, int 安全上限, CPU 库存材料可用量).
     */
    @Unique
    private long ae2e$computeBatch(ICraftingPatternDetails details, InventoryCrafting table) {
        Object progress = this.tasks.get(details);
        if (progress == null) {
            return 1;
        }
        long remaining = ((ITaskProgressAccessor) progress).ae2e$getValue();
        if (remaining <= 1) {
            return 1;
        }
        long n = remaining;
        // int 安全：每槽有效数量 × n 不得溢出 int。
        // 注意 ae2fc 会把 CPU 的 InventoryCrafting 包装成 FluidConvertingInventoryCrafting，
        // 流体/气体以 packet 形式存放（count=1，真实数量在 NBT 的 FluidStack.Amount /
        // GasStack.amount 中），必须按 NBT 数量做上限核算。
        for (int i = 0; i < table.getSizeInventory(); i++) {
            ItemStack s = table.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            long perSlot = ae2e$getEffectiveSlotAmount(s);
            if (perSlot > 0) {
                n = Math.min(n, Integer.MAX_VALUE / perSlot);
            }
        }
        if (n <= 1) {
            return 1;
        }
        IMEInventory<IAEItemStack> inv = this.getInventory();
        for (IAEItemStack input : details.getInputs()) {
            if (input == null || input.getStackSize() <= 0) {
                continue;
            }
            IAEItemStack probe = input.copy();
            probe.setStackSize(input.getStackSize() * n);
            IAEItemStack avail = inv.extractItems(probe, Actionable.SIMULATE, this.machineSrc);
            long availSize = avail == null ? 0 : avail.getStackSize();
            n = Math.min(n, availSize / input.getStackSize());
            if (n <= 1) {
                return 1;
            }
        }
        return n;
    }

    /**
     * 槽位的有效数量：ae2fc 流体/气体 packet 取 NBT 中的真实数量，其余取 count.
     */
    @Unique
    private static long ae2e$getEffectiveSlotAmount(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            if (tag.hasKey("FluidStack", 10)) {
                return tag.getCompoundTag("FluidStack").getInteger("Amount");
            }
            if (tag.hasKey("GasStack", 10)) {
                return tag.getCompoundTag("GasStack").getInteger("amount");
            }
        }
        return stack.getCount();
    }

    /**
     * 构造放大 table（原 table 保持 1 份不变，供 pushPattern 失败路径由原生代码原样回灌）.
     * 普通物品 count × batch；ae2fc 流体/气体 packet 放大 NBT 中的真实数量
     * （packet 的 count 恒为 1，数量存于 NBT，直接放大 count 会导致流体/气体只发 1 份——吞流体）。
     */
    @Unique
    private static InventoryCrafting ae2e$scaleTable(InventoryCrafting table, long batch) {
        int size = table.getSizeInventory();
        int dim = (int) Math.ceil(Math.sqrt(size));
        if (dim < 3) {
            dim = 3;
        }
        if (dim > 10) {
            dim = 10;
        }
        InventoryCrafting scaled = new InventoryCrafting(new ContainerNull(), dim, dim);
        for (int i = 0; i < size && i < scaled.getSizeInventory(); i++) {
            ItemStack s = table.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack c = s.copy();
            if (!ae2e$scalePacketAmount(c, batch)) {
                c.setCount((int) (s.getCount() * batch));
            }
            scaled.setInventorySlotContents(i, c);
        }
        return scaled;
    }

    /**
     * 放大 ae2fc 流体/气体 packet 的 NBT 数量.返回 true 表示该栈是 packet（已按 NBT 缩放，
     * 不应再缩放 count）。上限已由 computeBatch 的逐槽核算保证不溢出 int，此处 clamp 仅兜底。
     */
    @Unique
    private static boolean ae2e$scalePacketAmount(ItemStack stack, long batch) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        if (tag.hasKey("FluidStack", 10)) {
            NBTTagCompound fs = tag.getCompoundTag("FluidStack");
            long amount = fs.getInteger("Amount") * batch;
            fs.setInteger("Amount", (int) Math.min(amount, Integer.MAX_VALUE));
            return true;
        }
        if (tag.hasKey("GasStack", 10)) {
            NBTTagCompound gs = tag.getCompoundTag("GasStack");
            long amount = gs.getInteger("amount") * batch;
            gs.setInteger("amount", (int) Math.min(amount, Integer.MAX_VALUE));
            return true;
        }
        return false;
    }

    /**
     * 按原生逐份记账口径补记 extra 份产出预期，并扣除任务剩余数.
     */
    @Unique
    private void ae2e$correctCounts(ICraftingPatternDetails details, long extra) {
        Object progress = this.tasks.get(details);
        if (progress != null) {
            long remaining = ((ITaskProgressAccessor) progress).ae2e$getValue();
            ((ITaskProgressAccessor) progress).ae2e$setValue(Math.max(0, remaining - extra));
        }
        long totalOut = 0;
        for (IAEItemStack out : details.getCondensedOutputs()) {
            if (out != null && out.getStackSize() > 0) {
                totalOut += out.getStackSize() * extra;
            }
        }
        this.remainingItemCount -= totalOut;
        IItemList<IAEItemStack> waitingFor = this.waitingFor;
        if (waitingFor == null) {
            return;
        }
        for (IAEItemStack out : details.getCondensedOutputs()) {
            if (out == null || out.getStackSize() <= 0) {
                continue;
            }
            IAEItemStack extraOut = out.copy();
            extraOut.setStackSize(out.getStackSize() * extra);
            this.postChange(extraOut.copy(), this.machineSrc);
            waitingFor.add(extraOut);
            this.postCraftingStatusChange(extraOut.copy());
        }
    }
}
