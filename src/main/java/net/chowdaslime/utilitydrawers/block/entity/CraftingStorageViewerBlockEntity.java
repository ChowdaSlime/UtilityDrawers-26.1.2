package net.chowdaslime.utilitydrawers.block.entity;

import net.chowdaslime.utilitydrawers.menu.CraftingStorageViewerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CraftingStorageViewerBlockEntity extends StorageViewerBlockEntity {

    private NonNullList<ItemStack> craftGridContents = NonNullList.withSize(9, ItemStack.EMPTY);

    public CraftingStorageViewerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRAFTING_STORAGE_VIEWER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.utilitydrawers.crafting_storage_viewer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStorageViewerMenu(containerId, playerInventory, this.getStorageInterface(), this.getBlockPos(), this);
    }

    public NonNullList<ItemStack> getCraftGridContents() {
        return craftGridContents;
    }

    public void setCraftGridContents(NonNullList<ItemStack> contents) {
        this.craftGridContents = contents;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput list = output.child("CraftGrid");
        for (int i = 0; i < craftGridContents.size(); i++) {
            ItemStack stack = craftGridContents.get(i);
            if (!stack.isEmpty()) {
                list.child(String.valueOf(i)).store("Item", ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        craftGridContents = NonNullList.withSize(9, ItemStack.EMPTY);
        input.child("CraftGrid").ifPresent(list -> {
            for (int i = 0; i < 9; i++) {
                int idx = i;
                list.child(String.valueOf(i)).ifPresent(entry ->
                        craftGridContents.set(idx, entry.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY))
                );
            }
        });
    }
}