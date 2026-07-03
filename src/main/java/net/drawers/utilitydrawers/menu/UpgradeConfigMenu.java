package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.item.ExtractUpgradeItem;
import net.drawers.utilitydrawers.item.InsertUpgradeItem;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class UpgradeConfigMenu extends AbstractContainerMenu {

    private final ItemStack upgradeStack;
    private final SimpleContainer visibleFilter;

    private final DataSlot currentTab = DataSlot.standalone();
    private final DataSlot activeSidesMask = DataSlot.standalone();

    public UpgradeConfigMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, InteractionHand.MAIN_HAND);
    }

    public UpgradeConfigMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.UPGRADE_CONFIG_MENU.get(), containerId);
        Player player = playerInventory.player;

        if (player.getMainHandItem().getItem() instanceof InsertUpgradeItem
                || player.getMainHandItem().getItem() instanceof ExtractUpgradeItem) {
            this.upgradeStack = player.getMainHandItem();
        } else {
            this.upgradeStack = player.getItemInHand(hand);
        }
        this.visibleFilter = new SimpleContainer(9);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(this.visibleFilter, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlot(currentTab);
        this.addDataSlot(activeSidesMask);

        if (!playerInventory.player.level().isClientSide()) {
            loadTab(0);
            updateActiveMask();
        }
    }

    public int getCurrentTab() {
        return currentTab.get();
    }

    public boolean isSideActive(int directionIndex) {
        return (activeSidesMask.get() & (1 << directionIndex)) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 5) {
            saveTab();
            currentTab.set(id);
            loadTab(id);
            return true;
        } else if (id == 10) {
            toggleCurrentSide();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId >= 0 && slotId < 9) {
            Slot slot = this.slots.get(slotId);
            ItemStack cursor = this.getCarried();

            if (containerInput == ContainerInput.PICKUP) {
                if (cursor.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.set(cursor.copyWithCount(1));
                }
            } else if (containerInput == ContainerInput.QUICK_MOVE) {
                slot.set(ItemStack.EMPTY);
            }

            saveTab();
            return;
        }

        if (containerInput == ContainerInput.SWAP && button == player.getInventory().getSelectedSlot()) {
            return;
        }
        if (slotId >= 0 && slotId < this.slots.size()
                && this.slots.get(slotId).getItem() == upgradeStack) {
            return;
        }

        super.clicked(slotId, button, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem() && index >= 9) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index < 36) {
                if (!this.moveItemStackTo(stackInSlot, 36, 45, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stackInSlot, 9, 36, false)) return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            saveTab();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == upgradeStack || player.getOffhandItem() == upgradeStack;
    }


    private NonNullList<ItemStack> getFullFilterList() {
        NonNullList<ItemStack> list = NonNullList.withSize(54, ItemStack.EMPTY);
        ItemContainerContents contents = upgradeStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(list);
        }
        return list;
    }

    private void loadTab(int tabIndex) {
        NonNullList<ItemStack> list = getFullFilterList();
        int startIndex = tabIndex * 9;
        for (int i = 0; i < 9; i++) {
            visibleFilter.setItem(i, list.get(startIndex + i).copy());
        }
    }

    private void saveTab() {
        NonNullList<ItemStack> list = getFullFilterList();
        int startIndex = currentTab.get() * 9;
        for (int i = 0; i < 9; i++) {
            list.set(startIndex + i, visibleFilter.getItem(i).copy());
        }
        upgradeStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    private void toggleCurrentSide() {
        List<Direction> currentActive = new ArrayList<>(
                upgradeStack.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of())
        );
        Direction targetDir = Direction.from3DDataValue(currentTab.get());

        if (currentActive.contains(targetDir)) {
            currentActive.remove(targetDir);
        } else {
            currentActive.add(targetDir);
        }

        upgradeStack.set(ModDataComponents.ACTIVE_DIRECTIONS, currentActive);
        updateActiveMask();
    }

    private void updateActiveMask() {
        List<Direction> active = upgradeStack.getOrDefault(ModDataComponents.ACTIVE_DIRECTIONS, List.of());
        int mask = 0;
        for (Direction dir : active) {
            mask |= (1 << dir.get3DDataValue());
        }
        activeSidesMask.set(mask);
    }
}