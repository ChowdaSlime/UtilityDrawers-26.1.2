package net.chowdaslime.utilitydrawers.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

public class AutoRefillResultSlot extends ResultSlot {
    private final CraftingStorageViewerMenu menu;
    private final CraftingContainer craftSlots;

    public AutoRefillResultSlot(CraftingStorageViewerMenu menu, Player player, CraftingContainer craftSlots, Container container, int id, int x, int y) {
        super(player, craftSlots, container, id, x, y);
        this.menu = menu;
        this.craftSlots = craftSlots;
    }

    @Override
    public void onTake(Player player, ItemStack carried) {
        ItemStack[] before = new ItemStack[craftSlots.getContainerSize()];
        for (int i = 0; i < before.length; i++) {
            before[i] = craftSlots.getItem(i).copy();
        }

        super.onTake(player, carried);

        if (!player.level().isClientSide()) {
            menu.refillCraftGridFromNetwork(before);
        }
    }
}