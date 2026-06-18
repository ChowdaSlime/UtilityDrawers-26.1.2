package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class DrawerBlockEntity extends BlockEntity {

    private final int slotCount;
    private final ItemStack[] storedStacks;
    private final long[] storedCounts;
    private final long[] maxCapacities;
    private boolean locked = false;
    private BlockPos connectedInterface;

    private final ItemStack[] upgradeSlots = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWER_BLOCK_ENTITY.get(), pos, state);
        this.slotCount = (state.getBlock() instanceof DrawerBlock provider) ? provider.getSlotCount() : 1;
        this.storedStacks = new ItemStack[slotCount];
        this.storedCounts = new long[slotCount];
        this.maxCapacities = new long[slotCount];

        for (int i = 0; i < slotCount; i++) {
            storedStacks[i] = ItemStack.EMPTY;
            storedCounts[i] = 0L;
            maxCapacities[i] = (long) getBaseStackMultiplier() * 64;
        }
    }

    public boolean hasInterface() {
        return connectedInterface != null;
    }

    public BlockPos getConnectedInterface() {
        return connectedInterface;
    }

    public void setConnectedInterface(BlockPos pos) {
        connectedInterface = pos;
        setChanged();
    }

    public void clearConnectedInterface() {
        connectedInterface = null;
        setChanged();
    }

    public int getBaseStackMultiplier() {
        return switch (this.slotCount) {
            case 1 -> 32;
            case 2 -> 16;
            case 3 -> 10;
            case 4 -> 8;
            default -> 32;
        };
    }

    public int getSlotCount() {
        return slotCount;
    }

    public ItemStack getStoredItem(int slot) {
        return storedStacks[slot];
    }

    public long getStoredCount(int slot) {
        return storedCounts[slot];
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;

        if (!locked) {

            for (int i = 0; i < slotCount; i++) {

                if (storedCounts[i] <= 0) {
                    storedStacks[i] = ItemStack.EMPTY;

                    maxCapacities[i] =
                            (long) getBaseStackMultiplier()
                                    * 64
                                    * getUpgradeMultiplier();
                }
            }
        }

        setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(
                    this.getBlockPos(),
                    this.getBlockState(),
                    this.getBlockState(),
                    3
            );
        }
    }


    public boolean isSlotEmpty(int slot) {
        return storedStacks[slot].isEmpty() || storedCounts[slot] <= 0;
    }

    public boolean hasTemplate(int slot) {
        return !storedStacks[slot].isEmpty();
    }

    public void setTemplate(int slot, ItemStack stack) {
        storedStacks[slot] = stack.copyWithCount(1);
        storedCounts[slot] = 0;

        maxCapacities[slot] =
                (long) getBaseStackMultiplier()
                        * stack.getMaxStackSize()
                        * getUpgradeMultiplier();

        setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(
                    this.getBlockPos(),
                    this.getBlockState(),
                    this.getBlockState(),
                    3
            );
        }
    }

    public boolean hasVoidUpgrade() {
        for (ItemStack upgrade : upgradeSlots) {
            if (upgrade.getItem() instanceof VoidUpgradeItem) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIntoSlot(int slot, ItemStack stack, boolean simulate) {
        if (stack.getItem() instanceof StorageRemoteItem) {
            return stack;
        }

        if (storedStacks[slot].isEmpty()) {
            maxCapacities[slot] = (long) getBaseStackMultiplier() * stack.getMaxStackSize() * getUpgradeMultiplier();
        }

        if (locked && storedStacks[slot].isEmpty()) {
            return stack;
        }

        long space = maxCapacities[slot] - storedCounts[slot];
        if (space <= 0) return hasVoidUpgrade() ? ItemStack.EMPTY : stack;

        int toInsert = (int) Math.min(stack.getCount(), space);

        if (!simulate) {
            if (storedStacks[slot].isEmpty()) {
                storedStacks[slot] = stack.copyWithCount(1);
            }
            storedCounts[slot] += toInsert;
            setChanged();

            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            }
        }

        int remainder = stack.getCount() - toInsert;

        if (remainder > 0 && hasVoidUpgrade()) {
            return ItemStack.EMPTY;
        }
        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    public ItemStack insertItemIntoSlot(int slot, ItemStack stack, boolean simulate) {

        if (slot < 0 || slot >= slotCount)
            return stack;

        if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (!storedStacks[slot].isEmpty() && !ItemStack.isSameItemSameComponents(storedStacks[slot], stack)) {
            return stack;
        }
        return insertIntoSlot(slot, stack, simulate);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {

        if (slot < 0 || slot >= slotCount)
            return ItemStack.EMPTY;

        if (storedStacks[slot].isEmpty() || storedCounts[slot] <= 0)
            return ItemStack.EMPTY;

        int maxStackSize = storedStacks[slot].getMaxStackSize();
        int toExtract = (int) Math.min(amount, Math.min(storedCounts[slot], maxStackSize));

        if (toExtract <= 0)
            return ItemStack.EMPTY;

        ItemStack result = storedStacks[slot].copyWithCount(toExtract);

        if (!simulate) {
            storedCounts[slot] -= toExtract;
            if (storedCounts[slot] <= 0) {
                storedCounts[slot] = 0;

                if (!locked) {
                    storedStacks[slot] = ItemStack.EMPTY;
                    maxCapacities[slot] = (long) getBaseStackMultiplier() * 64 * getUpgradeMultiplier();
                }
            }
            setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            }
        }
        return result;
    }

    // Upgrades
    public ItemStack getUpgradeSlot(int slot) {
        return upgradeSlots[slot];
    }

    public boolean insertUpgrade(ItemStack upgrade) {
        for (int i = 0; i < 4; i++) {
            if (upgradeSlots[i].isEmpty()) {
                upgradeSlots[i] = upgrade.copyWithCount(1);
                recalculateCapacities();
                setChanged();
                if (this.level != null && !this.level.isClientSide()) {this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
                }
                return true;
            }
        }
        return false;
    }

    public boolean canRemoveUpgrade(int upgradeSlot) {
        if (upgradeSlots[upgradeSlot].isEmpty())
            return false;
        int newMultiplier = 1;
        for (int i = 0; i < 4; i++) {
            if (i == upgradeSlot) continue;

            if (!upgradeSlots[i].isEmpty() && upgradeSlots[i].getItem() instanceof DrawerUpgradeItem upgrade) {
                newMultiplier *= upgrade.getMultiplier();
            }
        }
        for (int i = 0; i < slotCount; i++) {
            if (!storedStacks[i].isEmpty()) {long newCapacity = (long) getBaseStackMultiplier() * storedStacks[i].getMaxStackSize() * newMultiplier;

                if (storedCounts[i] > newCapacity)
                    return false;
            }
        }
        return true;
    }


    public int getUpgradeMultiplier() {
        int multiplier = 1;
        for (ItemStack upgrade : upgradeSlots) {
            if (!upgrade.isEmpty() && upgrade.getItem() instanceof DrawerUpgradeItem upgradeItem) {
                multiplier *= upgradeItem.getMultiplier();
            }
        }
        return multiplier;
    }

    private void recalculateCapacities() {
        for (int i = 0; i < slotCount; i++) {
            int stackSize = storedStacks[i].isEmpty() ? 64 : storedStacks[i].getMaxStackSize();
            maxCapacities[i] = (long) getBaseStackMultiplier() * stackSize * getUpgradeMultiplier();
        }
    }

    public void setUpgradeSlot(int slot, ItemStack stack) {
        this.upgradeSlots[slot] = stack;
        recalculateCapacities();
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void unlinkFromInterfaces() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (connectedInterface == null) {
            return;
        }

        if (level.getBlockEntity(connectedInterface)
                instanceof StorageInterfaceBlockEntity storage) {
            storage.tryUnlinkDrawer(worldPosition);
        }
        connectedInterface = null;
    }

    // Save and Load
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Locked", locked);
        for (int i = 0; i < slotCount; i++) {
            ValueOutput slotOutput = output.child("Slot" + i);
            if (!storedStacks[i].isEmpty()) {
                slotOutput.store("Item", ItemStack.CODEC, storedStacks[i]);
                slotOutput.putLong("Count", storedCounts[i]);
            }
        }
        for (int i = 0; i < 4; i++) {
            ValueOutput upgradeOutput = output.child("Upgrade" + i);
            if (!upgradeSlots[i].isEmpty()) {
                upgradeOutput.store("Item", ItemStack.CODEC, upgradeSlots[i]);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        locked = input.getBooleanOr("Locked", false);
        for (int i = 0; i < slotCount; i++) {
            final int slot = i;
            storedStacks[slot] = ItemStack.EMPTY;
            storedCounts[slot] = 0;
            input.child("Slot" + slot).ifPresent(slotInput -> {
                storedStacks[slot] = slotInput.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                storedCounts[slot] = slotInput.getLongOr("Count", 0L);
            });
        }
        for (int i = 0; i < 4; i++) {
            final int upgradeIndex = i;
            upgradeSlots[upgradeIndex] = ItemStack.EMPTY;
            input.child("Upgrade" + upgradeIndex).ifPresent(upgradeInput -> {
                upgradeSlots[upgradeIndex] = upgradeInput.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            });
        }
        recalculateCapacities();
    }

    public void loadContentsFromTag(CompoundTag tag) {
        locked = tag.getBooleanOr("Locked", false);
        for (int i = 0; i < slotCount; i++) {
            storedStacks[i] = ItemStack.EMPTY;
            storedCounts[i] = 0L;
            if (tag.getCompound("Slot" + i).isPresent()) {
                CompoundTag slotTag = tag.getCompound("Slot" + i).orElseThrow();
                storedStacks[i] = slotTag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                storedCounts[i] = slotTag.getLongOr("Count", 0L);
            }
        }
        for (int i = 0; i < 4; i++) {
            upgradeSlots[i] = ItemStack.EMPTY;
            if (tag.getCompound("Upgrade" + i).isPresent()) {
                CompoundTag upgradeTag = tag.getCompound("Upgrade" + i).orElseThrow();
                upgradeSlots[i] = upgradeTag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
        }
        recalculateCapacities();
        setChanged();
    }

    public CompoundTag saveDrawerData(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Locked", locked);
        for (int i = 0; i < slotCount; i++) {
            if (!storedStacks[i].isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), storedStacks[i]).getOrThrow().copy());
                slotTag.putLong("Count", storedCounts[i]);
                tag.put("Slot" + i, slotTag);
            }
        }
        for (int i = 0; i < 4; i++) {
            if (!upgradeSlots[i].isEmpty()) {
                CompoundTag upgradeTag = new CompoundTag();
                upgradeTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), upgradeSlots[i]).getOrThrow().copy());
                tag.put("Upgrade" + i, upgradeTag);
            }
        }
        return tag;
    }

    // Renderer
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveDrawerData(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        if (this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
}