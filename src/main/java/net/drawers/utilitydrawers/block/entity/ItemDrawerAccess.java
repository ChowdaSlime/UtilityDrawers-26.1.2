package net.drawers.utilitydrawers.block.entity;

import net.minecraft.world.item.ItemStack;

public interface ItemDrawerAccess {
    int getSlotCount();
    ItemStack getStoredItem(int slot);
    boolean isSlotEmpty(int slot);
    ItemStack insertItemIntoSlot(int slot, ItemStack stack, boolean simulate);
    ItemStack extractItem(int slot, int amount, boolean simulate);
}