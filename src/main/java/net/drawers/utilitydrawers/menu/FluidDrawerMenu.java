package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluidDrawerMenu extends AbstractContainerMenu {

    private final FluidDrawerBlockEntity blockEntity;
    private final int slotCount;
    private final SimpleContainer upgradeContainer;
    private final boolean hasUpgrades;

    private boolean isInitializing = true;

    protected static final int UPGRADE_SLOT_COUNT = 4;

    public FluidDrawerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level()
                .getBlockEntity(buf.readBlockPos()));
    }

    public FluidDrawerMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        this(ModMenuTypes.FLUID_DRAWER_MENU.get(), containerId, playerInventory, blockEntity, true);
    }

    protected FluidDrawerMenu(MenuType<?> type, int containerId, Inventory playerInventory, BlockEntity blockEntity, boolean hasUpgrades) {
        super(type, containerId);

        this.blockEntity = (FluidDrawerBlockEntity) blockEntity;
        this.slotCount = this.blockEntity.getSlotCount();
        this.hasUpgrades = hasUpgrades;

        if (this.hasUpgrades) {
            this.upgradeContainer = new SimpleContainer(UPGRADE_SLOT_COUNT) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!FluidDrawerMenu.this.isInitializing) {
                        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                            FluidDrawerMenu.this.blockEntity.setUpgradeSlot(i, this.getItem(i).copy());
                        }
                    }
                }
            };

            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                this.upgradeContainer.setItem(i, this.blockEntity.getUpgradeSlot(i).copy());
            }

            this.isInitializing = false;

            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                int xPos = 152;
                int yPos = 8 + (i * 18);
                this.addSlot(new UpgradeSlot(this.upgradeContainer, i, xPos, yPos, this.blockEntity));
            }
        } else {
            this.upgradeContainer = null;
            this.isInitializing = false;
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

    public boolean hasUpgrades() {
        return this.hasUpgrades;
    }

    public FluidDrawerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getDrawerSlotCount() {
        return slotCount;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
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

            int uCount = this.hasUpgrades ? UPGRADE_SLOT_COUNT : 0;
            int invStart = uCount;
            int hotbarStart = uCount + 27;
            int invEnd = uCount + 36;

            if (this.hasUpgrades && index < uCount) {
                if (!this.moveItemStackTo(stackInSlot, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (this.hasUpgrades && (stackInSlot.getItem() instanceof DrawerUpgradeItem || stackInSlot.getItem() instanceof VoidUpgradeItem)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, uCount, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= invStart && index < hotbarStart) {
                    if (!this.moveItemStackTo(stackInSlot, hotbarStart, invEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= hotbarStart && index < invEnd) {
                    if (!this.moveItemStackTo(stackInSlot, invStart, hotbarStart, false)) {
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
        private final FluidDrawerBlockEntity blockEntity;

        public UpgradeSlot(SimpleContainer container, int index, int x, int y, FluidDrawerBlockEntity blockEntity) {
            super(container, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DrawerUpgradeItem || stack.getItem() instanceof VoidUpgradeItem;
        }

        @Override
        public boolean mayPickup(Player player) {
            return blockEntity.canRemoveUpgrade(this.getSlotIndex());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}