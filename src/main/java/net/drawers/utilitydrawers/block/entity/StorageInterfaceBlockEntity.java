package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.*;

public class StorageInterfaceBlockEntity extends BlockEntity {

    private final List<BlockPos> connectedDrawers = new ArrayList<>();
    private ItemStack upgradeSlot = ItemStack.EMPTY;

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
            } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
                compactingDrawer.setLocked(lockState);
            } else {
                it.remove();
                this.setChanged();
            }
        }
    }

    public List<BlockPos> getConnectedDrawers() {
        boolean removed = connectedDrawers.removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DrawerBlockEntity) && !(be instanceof FluidDrawerBlockEntity) && !(be instanceof CompactingDrawerBlockEntity);
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
        double dx = drawerPos.getX() - worldPosition.getX();
        double dy = drawerPos.getY() - worldPosition.getY();
        double dz = drawerPos.getZ() - worldPosition.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > getMaxRangeSq()) return false;

        BlockEntity be = level.getBlockEntity(drawerPos);

        if (be instanceof DrawerBlockEntity drawer) {
            if (drawer.hasInterface() || connectedDrawers.contains(drawerPos))
                return false;
            drawer.setConnectedInterface(worldPosition);
        } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
            if (fluidDrawer.hasInterface() || connectedDrawers.contains(drawerPos))
                return false;
            fluidDrawer.setConnectedInterface(worldPosition);
        } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
            if (compactingDrawer.hasInterface() || connectedDrawers.contains(drawerPos))
                return false;
            compactingDrawer.setConnectedInterface(worldPosition);
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
            } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
                compactingDrawer.clearConnectedInterface();
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
            } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
                compactingDrawer.clearConnectedInterface();
            }
        }
        connectedDrawers.clear();
        setChanged();
    }

    public ItemStack insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty() || level == null)
            return ItemStack.EMPTY;

        connectedDrawers.removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DrawerBlockEntity) && !(be instanceof FluidDrawerBlockEntity) && !(be instanceof CompactingDrawerBlockEntity);
        });

        ItemStack remainder = stack.copy();

        for (BlockPos pos : connectedDrawers) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    ItemStack stored = drawer.getStoredItem(i);
                    if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remainder)) {
                        remainder = drawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return ItemStack.EMPTY;
                    }
                }
            } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
                for (int i = 0; i < compactingDrawer.getSlotCount(); i++) {
                    ItemStack stored = compactingDrawer.getStoredItem(i);
                    if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remainder)) {
                        remainder = compactingDrawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return ItemStack.EMPTY;
                    }
                }
            }
        }

        for (BlockPos pos : connectedDrawers) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    if (drawer.isSlotEmpty(i) && (!drawer.isLocked() || drawer.hasTemplate(i))) {
                        remainder = drawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return ItemStack.EMPTY;
                    }
                }
            } else if (be instanceof CompactingDrawerBlockEntity compactingDrawer) {
                for (int i = 0; i < compactingDrawer.getSlotCount(); i++) {
                    if (compactingDrawer.isSlotEmpty(i) && !compactingDrawer.isLocked()) {
                        remainder = compactingDrawer.insertItemIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return ItemStack.EMPTY;
                    }
                }
            }
        }
        return remainder;
    }

    public FluidStack insertFluidIntoNetwork(FluidStack stack) {
        if (stack.isEmpty() || level == null)
            return stack;

        FluidStack remainder = stack.copy();

        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    FluidStack stored = fluidDrawer.getStoredFluid(i);
                    if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, remainder)) {
                        remainder = fluidDrawer.insertFluidIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return FluidStack.EMPTY;
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
                        if (remainder.isEmpty())
                            return FluidStack.EMPTY;
                    }
                }
            }
        }

        for (BlockPos pos : connectedDrawers) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    if (fluidDrawer.isSlotEmpty(i) && !fluidDrawer.isLocked()) {
                        remainder = fluidDrawer.insertFluidIntoSlot(i, remainder, false);
                        if (remainder.isEmpty())
                            return FluidStack.EMPTY;
                    }
                }
            }
        }

        return remainder;
    }

    public ItemStack getUpgradeSlot() {
        return upgradeSlot;
    }

    public void setUpgradeSlot(ItemStack stack) {
        this.upgradeSlot = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getRangeMultiplier() {
        if (!upgradeSlot.isEmpty() && upgradeSlot.getItem() instanceof DrawerUpgradeItem upgrade) {
            return upgrade.getMultiplier();
        }
        return 1;
    }

    public double getMaxRangeSq() {
        double range = 16.0 * getRangeMultiplier();
        return range * range;
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

        if (!upgradeSlot.isEmpty()) {
            output.child("UpgradeSlot").store("Item", ItemStack.CODEC, upgradeSlot);
        }
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
        upgradeSlot = ItemStack.EMPTY;
        input.child("UpgradeSlot").ifPresent(u ->
                upgradeSlot = u.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ResourceHandler<ItemResource> createItemHandler() {
        record SlotRef(ResourceHandler<ItemResource> handler, int slot) {}

        return new ResourceHandler<ItemResource>() {

            private List<SlotRef> allSlots() {
                List<SlotRef> slots = new ArrayList<>();
                for (BlockPos pos : getConnectedDrawers()) {
                    if (level == null) continue;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof DrawerBlockEntity drawer) {
                        ResourceHandler<ItemResource> handler = drawer.createItemHandler();
                        for (int i = 0; i < drawer.getSlotCount(); i++)
                            slots.add(new SlotRef(handler, i));
                    } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                        ResourceHandler<ItemResource> handler = compacting.createItemHandler();
                        for (int i = 0; i < compacting.getSlotCount(); i++)
                            slots.add(new SlotRef(handler, i));
                    }
                }
                return slots;
            }

            @Override
            public int size() {
                return allSlots().size();
            }

            @Override
            public ItemResource getResource(int index) {
                var slots = allSlots();
                if (index >= slots.size())
                    return ItemResource.EMPTY;
                var ref = slots.get(index);
                return ref.handler().getResource(ref.slot());
            }

            @Override
            public long getAmountAsLong(int index) {
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().getAmountAsLong(ref.slot());
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().getCapacityAsLong(ref.slot(), resource);
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                var slots = allSlots();
                if (index >= slots.size())
                    return false;
                var ref = slots.get(index);
                return ref.handler().isValid(ref.slot(), resource);
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
                if (resource.isEmpty() || amount <= 0)
                    return 0;
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().insert(ref.slot(), resource, amount, tx);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
                if (resource.isEmpty() || amount <= 0)
                    return 0;
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().extract(ref.slot(), resource, amount, tx);
            }
        };
    }

    public ResourceHandler<FluidResource> createFluidHandler() {
        record SlotRef(ResourceHandler<FluidResource> handler, int slot) {}

        return new ResourceHandler<FluidResource>() {

            private List<SlotRef> allSlots() {
                List<SlotRef> slots = new ArrayList<>();
                for (BlockPos pos : getConnectedDrawers()) {
                    if (level == null) continue;
                    if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity fluidDrawer) {
                        ResourceHandler<FluidResource> handler = fluidDrawer.createFluidHandler();
                        for (int i = 0; i < fluidDrawer.getSlotCount(); i++)
                            slots.add(new SlotRef(handler, i));
                    }
                }
                return slots;
            }

            @Override
            public int size() {
                return allSlots().size();
            }

            @Override
            public FluidResource getResource(int index) {
                var slots = allSlots();
                if (index >= slots.size())
                    return FluidResource.EMPTY;
                var ref = slots.get(index);
                return ref.handler().getResource(ref.slot());
            }

            @Override
            public long getAmountAsLong(int index) {
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().getAmountAsLong(ref.slot());
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().getCapacityAsLong(ref.slot(), resource);
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                var slots = allSlots();
                if (index >= slots.size())
                    return false;
                var ref = slots.get(index);
                return ref.handler().isValid(ref.slot(), resource);
            }

            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
                if (resource.isEmpty() || amount <= 0)
                    return 0;
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().insert(ref.slot(), resource, amount, tx);
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
                if (resource.isEmpty() || amount <= 0)
                    return 0;
                var slots = allSlots();
                if (index >= slots.size())
                    return 0;
                var ref = slots.get(index);
                return ref.handler().extract(ref.slot(), resource, amount, tx);
            }
        };
    }
}