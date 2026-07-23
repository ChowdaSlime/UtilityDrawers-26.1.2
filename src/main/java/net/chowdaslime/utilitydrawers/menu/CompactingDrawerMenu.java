package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.chowdaslime.utilitydrawers.item.DrawerUpgradeItem;
import net.chowdaslime.utilitydrawers.item.ExtractUpgradeItem;
import net.chowdaslime.utilitydrawers.item.InsertUpgradeItem;
import net.chowdaslime.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CompactingDrawerMenu extends AbstractContainerMenu {

    private final CompactingDrawerBlockEntity blockEntity;
    private final SimpleContainer upgradeContainer;
    private final boolean hasUpgrades;

    private static final int UTILITY_SLOT_COUNT = 3;
    private static final int TIER_SLOT_COUNT = 4;
    private static final int TOTAL_UPGRADE_SLOTS = UTILITY_SLOT_COUNT + TIER_SLOT_COUNT;

    private boolean isInitializing = true;

    public CompactingDrawerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public CompactingDrawerMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        this(ModMenuTypes.COMPACTING_DRAWER_MENU.get(), containerId, playerInventory, blockEntity, true);
    }

    protected CompactingDrawerMenu(MenuType<?> type, int containerId, Inventory playerInventory, BlockEntity blockEntity, boolean hasUpgrades) {
        super(type, containerId);

        this.blockEntity = (CompactingDrawerBlockEntity) blockEntity;
        this.hasUpgrades = hasUpgrades;

        if (this.hasUpgrades) {
            this.upgradeContainer = new SimpleContainer(TOTAL_UPGRADE_SLOTS) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!CompactingDrawerMenu.this.isInitializing) {
                        for (int i = 0; i < TOTAL_UPGRADE_SLOTS; i++) {
                            CompactingDrawerMenu.this.blockEntity.setUpgradeSlot(i, this.getItem(i).copy());
                        }
                    }
                }
            };

            for (int i = 0; i < TOTAL_UPGRADE_SLOTS; i++) {
                this.upgradeContainer.setItem(i, this.blockEntity.getUpgradeSlot(i).copy());
            }
            this.isInitializing = false;

            for (int i = 0; i < UTILITY_SLOT_COUNT; i++) {
                this.addSlot(new UtilityUpgradeSlot(this.upgradeContainer, i, 8, 16 + (i * 18), this.blockEntity));
            }

            for (int i = 0; i < TIER_SLOT_COUNT; i++) {
                this.addSlot(new TierUpgradeSlot(this.upgradeContainer, i + UTILITY_SLOT_COUNT, 152, 8 + (i * 18), this.blockEntity));
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
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            int uCount = this.hasUpgrades ? TOTAL_UPGRADE_SLOTS : 0;
            int invStart = uCount;
            int hotbarStart = uCount + 27;
            int invEnd = uCount + 36;

            if (this.hasUpgrades && index < uCount) {
                if (!this.moveItemStackTo(stackInSlot, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (this.hasUpgrades && isUtilityUpgrade(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, UTILITY_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.hasUpgrades && stackInSlot.getItem() instanceof DrawerUpgradeItem) {
                    if (!this.moveItemStackTo(stackInSlot, UTILITY_SLOT_COUNT, TOTAL_UPGRADE_SLOTS, false)) {
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

    public static boolean isUtilityUpgrade(ItemStack stack) {
        return stack.getItem() instanceof VoidUpgradeItem ||
                stack.getItem() instanceof InsertUpgradeItem ||
                stack.getItem() instanceof ExtractUpgradeItem;
    }

    private static class UtilityUpgradeSlot extends Slot {
        private final CompactingDrawerBlockEntity blockEntity;

        public UtilityUpgradeSlot(SimpleContainer container, int index, int x, int y, CompactingDrawerBlockEntity blockEntity) {
            super(container, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isUtilityUpgrade(stack);
        }

        @Override public boolean mayPickup(Player player) {
            return blockEntity.canRemoveUpgrade(this.getSlotIndex());
        }

        @Override public int getMaxStackSize() { return 1; }
    }

    private static class TierUpgradeSlot extends Slot {
        private final CompactingDrawerBlockEntity blockEntity;

        public TierUpgradeSlot(SimpleContainer container, int index, int x, int y, CompactingDrawerBlockEntity blockEntity) {
            super(container, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DrawerUpgradeItem;
        }

        @Override public boolean mayPickup(Player player) {
            return blockEntity.canRemoveUpgrade(this.getSlotIndex());
        }

        @Override public int getMaxStackSize() { return 1; }
    }
}