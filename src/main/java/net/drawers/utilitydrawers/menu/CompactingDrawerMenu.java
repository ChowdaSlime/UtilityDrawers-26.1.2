package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CompactingDrawerMenu extends AbstractContainerMenu {

    private final CompactingDrawerBlockEntity blockEntity;
    private final SimpleContainer upgradeContainer;

    private static final int UPGRADE_SLOT_COUNT = 4;
    private boolean isInitializing = true;

    public CompactingDrawerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public CompactingDrawerMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.COMPACTING_DRAWER_MENU.get(), containerId);

        this.blockEntity = (CompactingDrawerBlockEntity) blockEntity;

        this.upgradeContainer = new SimpleContainer(UPGRADE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!CompactingDrawerMenu.this.isInitializing) {
                    for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                        CompactingDrawerMenu.this.blockEntity
                                .setUpgradeSlot(i, this.getItem(i).copy());
                    }
                }
            }
        };

        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            this.upgradeContainer.setItem(i, this.blockEntity.getUpgradeSlot(i).copy());
        }
        this.isInitializing = false;

        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            this.addSlot(new UpgradeSlot(this.upgradeContainer, i, 152, 8 + i * 18, this.blockEntity));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public CompactingDrawerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index < UPGRADE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, UPGRADE_SLOT_COUNT, UPGRADE_SLOT_COUNT + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stackInSlot.getItem() instanceof DrawerUpgradeItem
                    || stackInSlot.getItem() instanceof VoidUpgradeItem) {
                if (!this.moveItemStackTo(stackInSlot, 0, UPGRADE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < UPGRADE_SLOT_COUNT + 27) {
                if (!this.moveItemStackTo(stackInSlot, UPGRADE_SLOT_COUNT + 27, UPGRADE_SLOT_COUNT + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stackInSlot, UPGRADE_SLOT_COUNT, UPGRADE_SLOT_COUNT + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stackInSlot.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stackInSlot);
        return original;
    }

    private static class UpgradeSlot extends Slot {
        private final CompactingDrawerBlockEntity blockEntity;

        public UpgradeSlot(SimpleContainer container, int index, int x, int y,
                           CompactingDrawerBlockEntity blockEntity) {
            super(container, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DrawerUpgradeItem
                    || stack.getItem() instanceof VoidUpgradeItem;
        }

        @Override public boolean mayPickup(Player player) {
            return blockEntity.canRemoveUpgrade(this.getSlotIndex());
        }

        @Override public int getMaxStackSize() { return 1; }
    }
}