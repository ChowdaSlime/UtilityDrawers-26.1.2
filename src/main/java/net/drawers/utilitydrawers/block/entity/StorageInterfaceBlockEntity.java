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
import net.neoforged.neoforge.fluids.FluidStack;

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
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DrawerBlockEntity drawer) {
                drawer.setLocked(lockState);
            } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                fluidDrawer.setLocked(lockState);
            } else {
                it.remove();
                this.setChanged();
            }
        }
    }

    public List<BlockPos> getConnectedDrawers() {
        boolean removed = connectedDrawers.removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DrawerBlockEntity) && !(be instanceof FluidDrawerBlockEntity);
        });

        if (removed) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        return List.copyOf(connectedDrawers);
    }

    public boolean tryLinkDrawer(BlockPos drawerPos) {
        BlockEntity be = level.getBlockEntity(drawerPos);

        if (be instanceof DrawerBlockEntity drawer) {
            if (drawer.hasInterface() || connectedDrawers.contains(drawerPos)) return false;
            drawer.setConnectedInterface(worldPosition);
        } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
            if (fluidDrawer.hasInterface() || connectedDrawers.contains(drawerPos)) return false;
            fluidDrawer.setConnectedInterface(worldPosition);
        } else {
            return false;
        }

        connectedDrawers.add(drawerPos);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public boolean tryUnlinkDrawer(BlockPos drawerPos) {
        if (connectedDrawers.remove(drawerPos)) {
            BlockEntity be = level.getBlockEntity(drawerPos);
            if (be instanceof DrawerBlockEntity drawer) {
                drawer.clearConnectedInterface();
            } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                fluidDrawer.clearConnectedInterface();
            }
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public void unlinkAllDrawers() {
        if (level == null) return;
        for (BlockPos drawerPos : connectedDrawers) {
            BlockEntity be = level.getBlockEntity(drawerPos);
            if (be instanceof DrawerBlockEntity drawer) {
                drawer.clearConnectedInterface();
            } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                fluidDrawer.clearConnectedInterface();
            }
        }
        connectedDrawers.clear();
        setChanged();
    }

    public ItemStack insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty() || level == null) return ItemStack.EMPTY;

        connectedDrawers.removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DrawerBlockEntity) && !(be instanceof FluidDrawerBlockEntity);
        });

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

    public FluidStack insertFluidIntoNetwork(FluidStack stack) {
        if (stack.isEmpty() || level == null) return stack;

        FluidStack remainder = stack.copy();

        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    FluidStack stored = fluidDrawer.getStoredFluid(i);
                    if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, remainder)) {
                        remainder = fluidDrawer.insertFluidIntoSlot(i, remainder, false);
                        if (remainder.isEmpty()) return FluidStack.EMPTY;
                    }
                }
            }
        }

        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    if (fluidDrawer.isSlotEmpty(i) && fluidDrawer.isLocked() &&
                            fluidDrawer.hasTemplate(i) &&
                            FluidStack.isSameFluidSameComponents(fluidDrawer.getStoredFluid(i), remainder)) {
                        remainder = fluidDrawer.insertFluidIntoSlot(i, remainder, false);
                        if (remainder.isEmpty()) return FluidStack.EMPTY;
                    }
                }
            }
        }

        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    if (fluidDrawer.isSlotEmpty(i) && !fluidDrawer.isLocked()) {
                        remainder = fluidDrawer.insertFluidIntoSlot(i, remainder, false);
                        if (remainder.isEmpty()) return FluidStack.EMPTY;
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