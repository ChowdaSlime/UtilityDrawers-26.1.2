package net.drawers.utilitydrawers.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class StorageInterfaceBlockEntity extends BlockEntity {

    private final List<BlockPos> connectedDrawers = new ArrayList<>();

    public StorageInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_INTERFACE_BLOCK_ENTITY.get(), pos, state);
    }

    public void toggleNetworkLock(boolean lockState) {
        Iterator<BlockPos> it = connectedDrawers.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                drawer.setLocked(lockState);
            } else {
                it.remove();
                this.setChanged();
            }
        }
    }

    public List<BlockPos> getConnectedDrawers() {
        boolean removed = connectedDrawers.removeIf(
                pos -> !(level.getBlockEntity(pos) instanceof DrawerBlockEntity)
        );

        if (removed) {
            setChanged();

            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(
                        getBlockPos(),
                        getBlockState(),
                        getBlockState(),
                        3
                );
            }
        }

        return List.copyOf(connectedDrawers);
    }

    public boolean tryLinkDrawer(BlockPos drawerPos) {
        if (!connectedDrawers.contains(drawerPos)) {
            connectedDrawers.add(drawerPos);
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public boolean tryUnlinkDrawer(BlockPos drawerPos) {
        if (connectedDrawers.remove(drawerPos)) {
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public ItemStack insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty() || level == null) return ItemStack.EMPTY;

        Iterator<BlockPos> it = connectedDrawers.iterator();
        while (it.hasNext()) {
            if (!(level.getBlockEntity(it.next()) instanceof DrawerBlockEntity)) {
                it.remove();
                this.setChanged();
            }
        }

        ItemStack remainder = stack.copy();
        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    ItemStack stored = drawer.getStoredItem(i);
                    if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remainder)) {
                        remainder = drawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
        }
        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    if (drawer.isSlotEmpty(i) && (!drawer.isLocked() || drawer.hasTemplate(i))) {
                        remainder = drawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
        }
        return remainder;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        net.minecraft.world.level.storage.ValueOutput list = output.child("ConnectedDrawers");
        for (int i = 0; i < connectedDrawers.size(); i++) {
            BlockPos pos = connectedDrawers.get(i);
            list.child(String.valueOf(i)).putLong("Pos", pos.asLong());
        }
        list.putInt("Count", connectedDrawers.size());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        connectedDrawers.clear();
        input.child("ConnectedDrawers").ifPresent(list -> {
            int count = list.getIntOr("Count", 0);
            for (int i = 0; i < count; i++) {
                list.child(String.valueOf(i)).ifPresent(entry -> {
                    long pos = entry.getLongOr("Pos", 0L);
                    if (pos != 0L) connectedDrawers.add(BlockPos.of(pos));
                });
            }
        });
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this);
    }
}