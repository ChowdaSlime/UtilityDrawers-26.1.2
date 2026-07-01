package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.data.WirelessNetworkSavedData;
import net.drawers.utilitydrawers.menu.WirelessFluidDrawerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.*;

public class WirelessFluidDrawerBlockEntity extends FluidDrawerBlockEntity implements MenuProvider {

    private static final Map<String, Set<WirelessFluidDrawerBlockEntity>> LOADED_DRAWERS = new HashMap<>();
    private WirelessNetworkKey networkKey;

    public WirelessFluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_FLUID_DRAWER_BLOCK_ENTITY.get(), pos, state);
        this.networkKey = new WirelessNetworkKey(0, 0, 0, true, Optional.empty(), this.getSlotCount());
    }

    public WirelessNetworkKey getNetworkKey() {
        return networkKey;
    }

    public void setNetworkKey(WirelessNetworkKey key) {
        if (this.networkKey != null && level != null && !level.isClientSide()) {
            Set<WirelessFluidDrawerBlockEntity> old = LOADED_DRAWERS.get(this.networkKey.toKey());
            if (old != null) {
                old.remove(this);
                if (old.isEmpty()) LOADED_DRAWERS.remove(this.networkKey.toKey());
            }
        }
        this.networkKey = key;
        if (level != null && !level.isClientSide()) {
            LOADED_DRAWERS.computeIfAbsent(key.toKey(), k -> new HashSet<>()).add(this);
        }
        setChanged();
        syncToClient();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && networkKey != null) {
            LOADED_DRAWERS.computeIfAbsent(networkKey.toKey(), k -> new HashSet<>()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (networkKey != null) {
            Set<WirelessFluidDrawerBlockEntity> set = LOADED_DRAWERS.get(networkKey.toKey());
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) LOADED_DRAWERS.remove(networkKey.toKey());
            }
        }
    }

    private void notifyNetwork() {
        Set<WirelessFluidDrawerBlockEntity> peers = LOADED_DRAWERS.get(networkKey.toKey());
        if (peers == null)
            return;
        for (WirelessFluidDrawerBlockEntity other : peers) {
            if (other != this) other.syncToClient();
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean isLocked() {
        if (level == null || level.isClientSide())
            return super.isLocked();
        return WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount).locked;
    }

    @Override
    public void setLocked(boolean locked) {
        if (level == null || level.isClientSide()) {
            super.setLocked(locked);
            return;
        }
        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkFluidState state = data.getFluidNetwork(networkKey, slotCount);
        state.locked = locked;
        if (!locked) {
            for (int i = 0; i < slotCount; i++) {
                if (state.amounts[i] <= 0) state.fluids[i] = FluidStack.EMPTY;
            }
        }
        data.setDirty();
        notifyNetwork();
        syncToClient();
    }

    @Override
    public FluidStack insertFluidIntoSlot(int slot, FluidStack stack, boolean simulate) {
        if (slot < 0 || slot >= slotCount || stack.isEmpty())
            return stack;
        if (level == null || level.isClientSide())
            return super.insertFluidIntoSlot(slot, stack, simulate);

        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkFluidState state = data.getFluidNetwork(networkKey, slotCount);

        if (!state.fluids[slot].isEmpty() && !FluidStack.isSameFluidSameComponents(state.fluids[slot], stack))
            return stack;
        if (state.locked && state.fluids[slot].isEmpty()) return stack;

        long capacity = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY;
        long space = capacity - state.amounts[slot];
        if (space <= 0) return stack;

        int toInsert = (int) Math.min(stack.getAmount(), space);
        if (!simulate) {
            if (state.fluids[slot].isEmpty()) state.fluids[slot] = stack.copyWithAmount(1);
            state.amounts[slot] += toInsert;
            data.setDirty();
            notifyNetwork();
            syncToClient();
        }
        int remainder = stack.getAmount() - toInsert;
        return remainder <= 0 ? FluidStack.EMPTY : stack.copyWithAmount(remainder);
    }

    @Override
    public FluidStack extractFluid(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount)
            return FluidStack.EMPTY;
        if (level == null || level.isClientSide())
            return super.extractFluid(slot, amount, simulate);

        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkFluidState state = data.getFluidNetwork(networkKey, slotCount);

        if (state.fluids[slot].isEmpty() || state.amounts[slot] <= 0)
            return FluidStack.EMPTY;

        int toExtract = (int) Math.min(amount, state.amounts[slot]);
        if (toExtract <= 0)
            return FluidStack.EMPTY;

        FluidStack result = state.fluids[slot].copyWithAmount(toExtract);
        if (!simulate) {
            state.amounts[slot] -= toExtract;
            if (state.amounts[slot] <= 0) {
                state.amounts[slot] = 0;
                if (!state.locked) state.fluids[slot] = FluidStack.EMPTY;
            }
            data.setDirty();
            notifyNetwork();
            syncToClient();
        }
        return result;
    }

    @Override
    public FluidStack getStoredFluid(int slot) {
        if (level == null || level.isClientSide())
            return super.getStoredFluid(slot);
        return WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount).fluids[slot];
    }

    @Override
    public long getStoredAmount(int slot) {
        if (level == null || level.isClientSide())
            return super.getStoredAmount(slot);
        return WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount).amounts[slot];
    }

    @Override
    public boolean isSlotEmpty(int slot) {
        if (level == null || level.isClientSide())
            return super.isSlotEmpty(slot);
        WirelessNetworkSavedData.NetworkFluidState state = WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount);
        return state.fluids[slot].isEmpty() || state.amounts[slot] <= 0;
    }

    @Override
    public long getMaxCapacity(int slot) {
        if (level == null || level.isClientSide())
            return super.getMaxCapacity(slot);
        return (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY;
    }

    @Override
    public int getBaseStackMultiplier() {
        return super.getBaseStackMultiplier() * 2;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (networkKey != null) {
            output.putInt("NetColor1", networkKey.color1());
            output.putInt("NetColor2", networkKey.color2());
            output.putInt("NetColor3", networkKey.color3());
            output.putBoolean("NetPublic", networkKey.isPublic());
            networkKey.owner().ifPresent(uuid -> output.putString("NetOwner", uuid.toString()));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int c1 = input.getIntOr("NetColor1", 0);
        int c2 = input.getIntOr("NetColor2", 0);
        int c3 = input.getIntOr("NetColor3", 0);
        boolean pub = input.getBooleanOr("NetPublic", true);
        String ownerStr = input.getStringOr("NetOwner", "");
        Optional<UUID> owner = ownerStr.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(ownerStr));
        this.networkKey = new WirelessNetworkKey(c1, c2, c3, pub, owner, this.getSlotCount());
    }

    @Override
    public CompoundTag saveDrawerData(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        boolean isNetLocked = isLocked();
        if (connectedInterface != null) tag.putLong("ConnectedInterface", connectedInterface.asLong());
        if (level != null && !level.isClientSide()) {
            WirelessNetworkSavedData.NetworkFluidState state = WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount);
            isNetLocked = state.locked;
            for (int i = 0; i < Math.min(slotCount, state.fluids.length); i++) {
                if (!state.fluids[i].isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.put("Fluid", FluidStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), state.fluids[i]).getOrThrow().copy());
                    slotTag.putLong("Amount", state.amounts[i]);
                    tag.put("Slot" + i, slotTag);
                }
            }
        }
        tag.putBoolean("Locked", isNetLocked);

        if (this.networkKey != null) {
            tag.putInt("NetColor1", this.networkKey.color1());
            tag.putInt("NetColor2", this.networkKey.color2());
            tag.putInt("NetColor3", this.networkKey.color3());
            tag.putBoolean("NetPublic", this.networkKey.isPublic());
            this.networkKey.owner().ifPresent(uuid -> tag.putString("NetOwner", uuid.toString()));
        }

        return tag;
    }

    @Override
    public void loadContentsFromTag(CompoundTag tag) {
        super.loadContentsFromTag(tag);
        int c1 = tag.getIntOr("NetColor1", 0);
        int c2 = tag.getIntOr("NetColor2", 0);
        int c3 = tag.getIntOr("NetColor3", 0);
        boolean pub = tag.getBooleanOr("NetPublic", true);
        String ownerStr = tag.getStringOr("NetOwner", "");
        Optional<UUID> owner = ownerStr.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(ownerStr));
        this.networkKey = new WirelessNetworkKey(c1, c2, c3, pub, owner, this.getSlotCount());
    }

    @Override
    public ResourceHandler<FluidResource> createFluidHandler() {
        return new WirelessFluidHandler();
    }

    private final class WirelessFluidHandler extends SnapshotJournal<WirelessFluidHandler.Snapshot> implements ResourceHandler<FluidResource> {
        record Snapshot(FluidStack[] fluids, long[] amounts) {}
        @Override public int size() { return slotCount; }
        @Override public FluidResource getResource(int slot) { return FluidResource.of(getStoredFluid(slot)); }
        @Override public long getAmountAsLong(int slot) { return getStoredAmount(slot); }
        @Override public long getCapacityAsLong(int slot, FluidResource resource) {
            return getMaxCapacity(slot);
        }

        @Override
        public boolean isValid(int slot, FluidResource resource) {
            FluidStack current = getStoredFluid(slot);
            return !resource.isEmpty() && (current.isEmpty() ? !isLocked() : FluidResource.of(current).equals(resource));
        }

        @Override
        public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
            updateSnapshots(tx);
            return amount - insertFluidIntoSlot(slot, resource.toStack(amount), false).getAmount();
        }

        @Override
        public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
            updateSnapshots(tx);
            return extractFluid(slot, amount, false).getAmount();
        }

        @Override protected Snapshot createSnapshot() {
            WirelessNetworkSavedData.NetworkFluidState state = WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount);
            return new Snapshot(Arrays.stream(state.fluids).map(FluidStack::copy).toArray(FluidStack[]::new), state.amounts.clone());
        }
        @Override protected void revertToSnapshot(Snapshot snapshot) {
            WirelessNetworkSavedData.NetworkFluidState state = WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount);
            System.arraycopy(snapshot.amounts(), 0, state.amounts, 0, slotCount);
            state.fluids = snapshot.fluids();
        }
        @Override protected void onRootCommit(Snapshot originalState) {
            WirelessNetworkSavedData.get((ServerLevel) level).setDirty();
            notifyNetwork();
            syncToClient();
        }
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.utilitydrawers.wireless_fluid_drawer"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WirelessFluidDrawerMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean hasTemplate(int slot) {
        if (level == null || level.isClientSide())
            return super.hasTemplate(slot);
        return !WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount).fluids[slot].isEmpty();
    }

    @Override
    public void setTemplate(int slot, FluidStack stack) {
        if (level == null || level.isClientSide()) { super.setTemplate(slot, stack);
            return;
        }
        WirelessNetworkSavedData.NetworkFluidState state = WirelessNetworkSavedData.get((ServerLevel) level).getFluidNetwork(networkKey, slotCount);
        state.fluids[slot] = stack.copyWithAmount(1);
        state.amounts[slot] = 0;
        WirelessNetworkSavedData.get((ServerLevel) level).setDirty();
        notifyNetwork();
        syncToClient();
    }
}