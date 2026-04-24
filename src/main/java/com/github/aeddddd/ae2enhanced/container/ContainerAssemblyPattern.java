package com.github.aeddddd.ae2enhanced.container;

import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerAssemblyPattern extends Container {

    private static final int PATTERN_X = 10;
    private static final int PATTERN_Y = 24;
    private static final int INV_X = 89;
    private static final int INV_Y = 152;
    private static final int HOTBAR_Y = 210;

    private final TileAssemblyController tile;
    private final int page;
    private final int patternSlotCount;

    public ContainerAssemblyPattern(IInventory playerInv, TileAssemblyController tile, int page, int patternPages) {
        this.tile = tile;
        this.page = page;
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        int startSlot = TileAssemblyController.UPGRADE_SLOTS
            + page * TileAssemblyController.PATTERN_SLOTS_PER_PAGE;
        // 使用服务端同步的 patternPages 计算边界，避免客户端 handler.getSlots() 未同步导致槽位映射错误
        int expectedTotalSlots = TileAssemblyController.UPGRADE_SLOTS
            + patternPages * TileAssemblyController.PATTERN_SLOTS_PER_PAGE;
        int endSlot = Math.min(startSlot + TileAssemblyController.PATTERN_SLOTS_PER_PAGE,
            expectedTotalSlots);

        this.patternSlotCount = endSlot - startSlot;

        // 样板槽：当前页 16×6=96 槽（实际受 handler.getSlots() 限制）
        for (int i = startSlot; i < endSlot; i++) {
            int localIndex = i - startSlot;
            int row = localIndex / 16;
            int col = localIndex % 16;
            final int handlerIndex = i;
            final IItemHandler handlerRef = handler;
            this.addSlotToContainer(new SlotItemHandler(handler, i,
                PATTERN_X + col * 20, PATTERN_Y + row * 20) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return 1;
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }

                /**
                 * 覆盖 putStack，正确处理 insertItem 返回值。
                 * 默认实现忽略返回值，导致 Container.mergeItemStack 认为物品已放入，
                 * 但实际上传入的 stack 引用未被消耗，源槽位（背包）不会被清空，
                 * 出现"样板还在背包中"的同步异常。
                 *
                 * 注意：必须先复制 stack 再调用 insertItem，因为 Forge 的 ItemStackHandler
                 * 在成功时可能直接将传入的 stack 引用存入内部列表。如果随后修改该 stack
                 * 的 count，会同时污染 itemHandler 中的数据，导致物品消失。
                 */
                @Override
                public void putStack(@Nonnull ItemStack stack) {
                    if (stack.isEmpty()) {
                        handlerRef.extractItem(handlerIndex, Integer.MAX_VALUE, false);
                        this.onSlotChanged();
                        return;
                    }
                    ItemStack toInsert = stack.copy();
                    ItemStack remainder = handlerRef.insertItem(handlerIndex, toInsert, false);
                    if (remainder.isEmpty()) {
                        stack.setCount(0);
                    } else {
                        stack.setCount(remainder.getCount());
                    }
                    this.onSlotChanged();
                }
            });
        }

        // 玩家背包 3行×9列
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                    INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInv, col,
                INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
                && playerIn.getDistanceSq(tile.getPos()) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            int patternEnd = this.patternSlotCount;
            int playerStart = patternEnd;
            int playerEnd = playerStart + 36;

            if (index < patternEnd) {
                // 从样板槽移到玩家背包
                if (!this.mergeItemStack(itemstack1, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移到样板槽
                if (!this.mergeItemStack(itemstack1, 0, patternEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public TileAssemblyController getTile() {
        return tile;
    }

    public int getPage() {
        return page;
    }
}
