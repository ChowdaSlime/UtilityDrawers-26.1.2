package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.data.WirelessNetworkSavedData;
import net.drawers.utilitydrawers.menu.WirelessDrawerMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.*;

public class WirelessDrawerBlockEntity extends DrawerBlockEntity implements MenuProvider {

    private static final Map<String, Set<WirelessDrawerBlockEntity>> LOADED_DRAWERS = new HashMap<>();
    private WirelessNetworkKey networkKey;

    public WirelessDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_DRAWER_BLOCK_ENTITY.get(), pos, state);
        this.networkKey = new WirelessNetworkKey(0, 0, 0, true, Optional.empty(), this.getSlotCount());
    }

    public WirelessNetworkKey getNetworkKey() { return networkKey; }

    public void setNetworkKey(WirelessNetworkKey key) {
        if (this.networkKey != null && level != null && !level.isClientSide()) {
            Set<WirelessDrawerBlockEntity> old = LOADED_DRAWERS.get(this.networkKey.toKey());
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
            Set<WirelessDrawerBlockEntity> set = LOADED_DRAWERS.get(networkKey.toKey());
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) LOADED_DRAWERS.remove(networkKey.toKey());
            }
        }
    }

    private void notifyNetwork() {
        Set<WirelessDrawerBlockEntity> peers = LOADED_DRAWERS.get(networkKey.toKey());
        if (peers == null) return;
        for (WirelessDrawerBlockEntity other : peers) {
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
        return WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount).locked;
    }

    @Override
    public void setLocked(boolean locked) {
        if (level == null || level.isClientSide()) {
            super.setLocked(locked);
            return;
        }
        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkItemState state = data.getItemNetwork(networkKey, slotCount);
        state.locked = locked;
        if (!locked) {
            for (int i = 0; i < slotCount; i++) {
                if (state.counts[i] <= 0) state.stacks[i] = ItemStack.EMPTY;
            }
        }
        data.setDirty();
        notifyNetwork();
        syncToClient();
    }

    @Override
    public ItemStack insertItemIntoSlot(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= slotCount || stack.isEmpty())
            return stack;
        if (level == null || level.isClientSide())
            return super.insertItemIntoSlot(slot, stack, simulate);

        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkItemState state = data.getItemNetwork(networkKey, slotCount);

        if (!state.stacks[slot].isEmpty() && !ItemStack.isSameItemSameComponents(state.stacks[slot], stack))
            return stack;
        if (state.locked && state.stacks[slot].isEmpty())
            return stack;

        long capacity = (long) getBaseStackMultiplier() * stack.getMaxStackSize() * getUpgradeMultiplier();
        long space = capacity - state.counts[slot];
        if (space <= 0)
            return hasVoidUpgrade() ? ItemStack.EMPTY : stack;

        int toInsert = (int) Math.min(stack.getCount(), space);
        if (!simulate) {
            if (state.stacks[slot].isEmpty()) state.stacks[slot] = stack.copyWithCount(1);
            state.counts[slot] += toInsert;
            data.setDirty();
            notifyNetwork();
            syncToClient();
        }
        int remainder = stack.getCount() - toInsert;
        if (remainder > 0 && hasVoidUpgrade())
            return ItemStack.EMPTY;
        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount)
            return ItemStack.EMPTY;
        if (level == null || level.isClientSide())
            return super.extractItem(slot, amount, simulate);

        WirelessNetworkSavedData data = WirelessNetworkSavedData.get((ServerLevel) level);
        WirelessNetworkSavedData.NetworkItemState state = data.getItemNetwork(networkKey, slotCount);

        if (state.stacks[slot].isEmpty() || state.counts[slot] <= 0)
            return ItemStack.EMPTY;

        int maxStack = state.stacks[slot].getMaxStackSize();
        int toExtract = (int) Math.min(amount, Math.min(state.counts[slot], maxStack));
        if (toExtract <= 0) return ItemStack.EMPTY;

        ItemStack result = state.stacks[slot].copyWithCount(toExtract);
        if (!simulate) {
            state.counts[slot] -= toExtract;
            if (state.counts[slot] <= 0) {
                state.counts[slot] = 0;
                if (!state.locked) state.stacks[slot] = ItemStack.EMPTY;
            }
            data.setDirty();
            notifyNetwork();
            syncToClient();
        }
        return result;
    }

    @Override
    public ItemStack getStoredItem(int slot) {
        if (level == null || level.isClientSide())
            return super.getStoredItem(slot);
        return WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount).stacks[slot];
    }

    @Override
    public long getStoredCount(int slot) {
        if (level == null || level.isClientSide())
            return super.getStoredCount(slot);
        return WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount).counts[slot];
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
        boolean isNetLocked = locked;
        if (connectedInterface != null) tag.putLong("ConnectedInterface", connectedInterface.asLong());

        if (level != null && !level.isClientSide()) {
            WirelessNetworkSavedData.NetworkItemState state = WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount);
            isNetLocked = state.locked;
            for (int i = 0; i < Math.min(slotCount, state.stacks.length); i++) {
                if (!state.stacks[i].isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), state.stacks[i]).getOrThrow().copy());
                    slotTag.putLong("Count", state.counts[i]);
                    tag.put("Slot" + i, slotTag);
                }
            }
        }
        tag.putBoolean("Locked", isNetLocked);
        for (int i = 0; i < 4; i++) {
            if (!upgradeSlots[i].isEmpty()) {
                CompoundTag upgradeTag = new CompoundTag();
                upgradeTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), upgradeSlots[i]).getOrThrow().copy());
                tag.put("Upgrade" + i, upgradeTag);
            }
        }

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
    public ResourceHandler<ItemResource> createItemHandler() {
        return new WirelessItemHandler();
    }

    private final class WirelessItemHandler extends SnapshotJournal<WirelessItemHandler.Snapshot> implements ResourceHandler<ItemResource> {
        record Snapshot(ItemStack[] stacks, long[] counts) {}
        @Override public int size() {
            return slotCount;
        }

        @Override public ItemResource getResource(int slot) {
            return ItemResource.of(getStoredItem(slot));
        }

        @Override public long getAmountAsLong(int slot) {
            return getStoredCount(slot);
        }

        @Override public long getCapacityAsLong(int slot, ItemResource resource) {
            return (level == null || level.isClientSide()) ? 0 : (long) getBaseStackMultiplier() * getStoredItem(slot).getMaxStackSize() * getUpgradeMultiplier();
        }

        @Override public boolean isValid(int slot, ItemResource resource) {
            ItemStack current = getStoredItem(slot);
            return !resource.isEmpty() && (current.isEmpty() ? !isLocked() : ItemResource.of(current).equals(resource));
        }

        @Override public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
            updateSnapshots(tx);
            return amount - insertItemIntoSlot(slot, resource.toStack(amount), false).getCount();
        }

        @Override public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
            updateSnapshots(tx);
            return extractItem(slot, amount, false).getCount();
        }

        @Override protected Snapshot createSnapshot() {
            WirelessNetworkSavedData.NetworkItemState state = WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount);
            return new Snapshot(Arrays.stream(state.stacks).map(ItemStack::copy).toArray(ItemStack[]::new), state.counts.clone());
        }

        @Override protected void revertToSnapshot(Snapshot snapshot) {
            WirelessNetworkSavedData.NetworkItemState state = WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount);
            System.arraycopy(snapshot.counts(), 0, state.counts, 0, slotCount);
            state.stacks = snapshot.stacks();
        }

        @Override protected void onRootCommit(Snapshot originalState) {
            WirelessNetworkSavedData.get((ServerLevel) level).setDirty();
            notifyNetwork();
            syncToClient();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.utilitydrawers.wireless_drawer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WirelessDrawerMenu(containerId, playerInventory, this);
    }

    @Override public boolean isSlotEmpty(int slot) {
        return getStoredItem(slot).isEmpty() || getStoredCount(slot) <= 0;

    }
    @Override public int getBaseStackMultiplier() {
        return super.getBaseStackMultiplier() * 2;
    }

    @Override public boolean hasTemplate(int slot) {
        return !getStoredItem(slot).isEmpty();
    }

    @Override public void setTemplate(int slot, ItemStack stack) {
        WirelessNetworkSavedData.NetworkItemState state = WirelessNetworkSavedData.get((ServerLevel) level).getItemNetwork(networkKey, slotCount);
        state.stacks[slot] = stack.copyWithCount(1);
        state.counts[slot] = 0;
        WirelessNetworkSavedData.get((ServerLevel) level).setDirty();
        notifyNetwork();
        syncToClient();
    }
}