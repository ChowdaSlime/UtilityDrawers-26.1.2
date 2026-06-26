package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.block.entity.DrawerFramerBlockEntity;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DrawerFramerMenu extends AbstractContainerMenu {

    private final DrawerFramerBlockEntity blockEntity;

    public DrawerFramerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public DrawerFramerMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.DRAWER_FRAMER_MENU.get(), containerId);
        this.blockEntity = (DrawerFramerBlockEntity) blockEntity;

        this.addSlot(new Slot(this.blockEntity, DrawerFramerBlockEntity.SLOT_SIDES, 121, 48) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
            @Override public int getMaxStackSize() { return 1; }
        });

        this.addSlot(new Slot(this.blockEntity, DrawerFramerBlockEntity.SLOT_FACE, 121, 88) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
            @Override public int getMaxStackSize() { return 1; }
        });

        this.addSlot(new Slot(this.blockEntity, DrawerFramerBlockEntity.SLOT_INPUT, 41, 68) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BlockItem bi &&
                        (bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedDrawerBlock ||
                                bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedFluidDrawerBlock ||
                                bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedCompactingDrawerBlock);
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        this.addSlot(new Slot(this.blockEntity, DrawerFramerBlockEntity.SLOT_OUTPUT, 198, 68) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public int getMaxStackSize() { return 1; }
        });

        this.addSlot(new Slot(this.blockEntity, DrawerFramerBlockEntity.SLOT_UPGRADE, 198, 28) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof DrawerUpgradeItem; }
            @Override public int getMaxStackSize() { return 1; }
        });

        int INV_START_X = 45;
        int INV_START_Y = 145;
        int HOTBAR_Y = 216;
        int SLOT_SPACING = 19;

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INV_START_X + col * SLOT_SPACING, INV_START_Y + row * SLOT_SPACING));

        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col,
                    INV_START_X + col * SLOT_SPACING, HOTBAR_Y));
    }

    public DrawerFramerBlockEntity getBlockEntity() { return blockEntity; }

    public int getProgress()    { return blockEntity.getProgress(); }
    public int getMaxProgress() { return blockEntity.getMaxProgress(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 5) {
                if (!this.moveItemStackTo(stack, 5, this.slots.size(), true))
                    return ItemStack.EMPTY;
            }
            else {
                if (stack.getItem() instanceof DrawerUpgradeItem) {
                    if (!this.moveItemStackTo(stack, DrawerFramerBlockEntity.SLOT_UPGRADE, DrawerFramerBlockEntity.SLOT_UPGRADE + 1, false))
                        return ItemStack.EMPTY;
                } else if (stack.getItem() instanceof BlockItem bi &&
                        (bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedDrawerBlock ||
                                bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedFluidDrawerBlock ||
                                bi.getBlock() instanceof net.drawers.utilitydrawers.block.FramedCompactingDrawerBlock)) {
                    if (!this.moveItemStackTo(stack, DrawerFramerBlockEntity.SLOT_INPUT, DrawerFramerBlockEntity.SLOT_INPUT + 1, false))
                        return ItemStack.EMPTY;
                } else if (stack.getItem() instanceof BlockItem) {
                    if (!this.moveItemStackTo(stack, DrawerFramerBlockEntity.SLOT_SIDES, DrawerFramerBlockEntity.SLOT_SIDES + 1, false))
                        if (!this.moveItemStackTo(stack, DrawerFramerBlockEntity.SLOT_FACE, DrawerFramerBlockEntity.SLOT_FACE + 1, false))
                            return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) < 64;
    }
}