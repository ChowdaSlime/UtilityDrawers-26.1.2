package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StorageInterfaceMenu extends AbstractContainerMenu {

    private final StorageInterfaceBlockEntity blockEntity;
    private final SimpleContainer upgradeContainer;

    private boolean isInitializing = true;

    public StorageInterfaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level()
                .getBlockEntity(buf.readBlockPos()));
    }

    public StorageInterfaceMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.STORAGE_INTERFACE_MENU.get(), containerId);

        this.blockEntity = (StorageInterfaceBlockEntity) blockEntity;

        this.upgradeContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!StorageInterfaceMenu.this.isInitializing) {
                    StorageInterfaceMenu.this.blockEntity
                            .setUpgradeSlot(this.getItem(0).copy());
                }
            }
        };

        this.upgradeContainer.setItem(0, this.blockEntity.getUpgradeSlot().copy());
        this.isInitializing = false;

        this.addSlot(new UpgradeSlot(this.upgradeContainer, 0, 152, 35, this.blockEntity));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public StorageInterfaceBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.getItem() instanceof DrawerUpgradeItem) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 1 && index < 28) {
                    if (!this.moveItemStackTo(stackInSlot, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 28 && index < 37) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }

        return originalStack;
    }

    private static class UpgradeSlot extends Slot {
        private final StorageInterfaceBlockEntity blockEntity;

        public UpgradeSlot(SimpleContainer container, int index, int x, int y,
                           StorageInterfaceBlockEntity blockEntity) {
            super(container, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DrawerUpgradeItem;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}