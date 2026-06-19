package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidDrawerBlockEntity extends BlockEntity {

    private final int slotCount;
    FluidStack[] storedFluids;
    long[] storedAmounts;
    private final long[] maxCapacities;
    private boolean locked = false;
    private BlockPos connectedInterface;

    private final ItemStack[] upgradeSlots = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    private static final int BASE_FLUID_CAPACITY = 4000;
    public long getMaxCapacity(int slot) {
        return maxCapacities[slot];
    }

    public FluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DRAWER_BLOCK_ENTITY.get(), pos, state);

        this.slotCount = (state.getBlock() instanceof SlotCountProvider provider) ? provider.getSlotCount() : 1;

        this.storedFluids = new FluidStack[slotCount];
        this.storedAmounts = new long[slotCount];
        this.maxCapacities = new long[slotCount];

        for (int i = 0; i < slotCount; i++) {
            storedFluids[i] = FluidStack.EMPTY;
            storedAmounts[i] = 0L;
            maxCapacities[i] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY;
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

    public FluidStack getStoredFluid(int slot) {
        return storedFluids[slot];
    }

    public long getStoredAmount(int slot) {
        return storedAmounts[slot];
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;

        if (!locked) {
            for (int i = 0; i < slotCount; i++) {
                if (storedAmounts[i] <= 0) {
                    storedFluids[i] = FluidStack.EMPTY;
                    maxCapacities[i] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * getUpgradeMultiplier();
                }
            }
        }

        setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isSlotEmpty(int slot) {
        return storedFluids[slot].isEmpty() || storedAmounts[slot] <= 0;
    }

    public boolean hasTemplate(int slot) {
        return !storedFluids[slot].isEmpty();
    }

    public void setTemplate(int slot, FluidStack stack) {
        storedFluids[slot] = stack.copyWithAmount(1);
        storedAmounts[slot] = 0;

        maxCapacities[slot] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * getUpgradeMultiplier();

        setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
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

    private FluidStack insertIntoSlot(int slot, FluidStack stack, boolean simulate) {
        if (storedFluids[slot].isEmpty()) {
            maxCapacities[slot] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * getUpgradeMultiplier();
        }

        if (locked && storedFluids[slot].isEmpty()) {
            return stack;
        }

        long space = maxCapacities[slot] - storedAmounts[slot];
        if (space <= 0) return hasVoidUpgrade() ? FluidStack.EMPTY : stack;

        int toInsert = (int) Math.min(stack.getAmount(), space);

        if (!simulate) {
            if (storedFluids[slot].isEmpty()) {
                storedFluids[slot] = stack.copyWithAmount(1);
            }
            storedAmounts[slot] += toInsert;
            setChanged();

            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            }
        }

        int remainder = stack.getAmount() - toInsert;

        if (remainder > 0 && hasVoidUpgrade()) {
            return FluidStack.EMPTY;
        }
        return remainder <= 0 ? FluidStack.EMPTY : stack.copyWithAmount(remainder);
    }

    public FluidStack insertFluidIntoSlot(int slot, FluidStack stack, boolean simulate) {
        if (slot < 0 || slot >= slotCount)
            return stack;

        if (stack.isEmpty())
            return FluidStack.EMPTY;

        if (!storedFluids[slot].isEmpty() && !FluidStack.isSameFluidSameComponents(storedFluids[slot], stack)) {
            return stack;
        }
        return insertIntoSlot(slot, stack, simulate);
    }

    public FluidStack extractFluid(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount)
            return FluidStack.EMPTY;

        if (storedFluids[slot].isEmpty() || storedAmounts[slot] <= 0)
            return FluidStack.EMPTY;

        int toExtract = (int) Math.min(amount, storedAmounts[slot]);

        if (toExtract <= 0)
            return FluidStack.EMPTY;

        FluidStack result = storedFluids[slot].copyWithAmount(toExtract);

        if (!simulate) {
            storedAmounts[slot] -= toExtract;
            if (storedAmounts[slot] <= 0) {
                storedAmounts[slot] = 0;

                if (!locked) {
                    storedFluids[slot] = FluidStack.EMPTY;
                    maxCapacities[slot] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * getUpgradeMultiplier();
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
                if (this.level != null && !this.level.isClientSide()) {
                    this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
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
            if (!storedFluids[i].isEmpty()) {
                long newCapacity = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * newMultiplier;

                if (storedAmounts[i] > newCapacity)
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
            maxCapacities[i] = (long) getBaseStackMultiplier() * BASE_FLUID_CAPACITY * getUpgradeMultiplier();
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

        if (level.getBlockEntity(connectedInterface) instanceof StorageInterfaceBlockEntity storage) {
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
            if (!storedFluids[i].isEmpty()) {
                slotOutput.store("Fluid", FluidStack.CODEC, storedFluids[i]);
                slotOutput.putLong("Amount", storedAmounts[i]);
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
            storedFluids[slot] = FluidStack.EMPTY;
            storedAmounts[slot] = 0;
            input.child("Slot" + slot).ifPresent(slotInput -> {
                storedFluids[slot] = slotInput.read("Fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY);
                storedAmounts[slot] = slotInput.getLongOr("Amount", 0L);
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
            storedFluids[i] = FluidStack.EMPTY;
            storedAmounts[i] = 0L;
            if (tag.getCompound("Slot" + i).isPresent()) {
                CompoundTag slotTag = tag.getCompound("Slot" + i).orElseThrow();
                storedFluids[i] = slotTag.read("Fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY);
                storedAmounts[i] = slotTag.getLongOr("Amount", 0L);
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
            if (!storedFluids[i].isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.put("Fluid", FluidStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), storedFluids[i]).getOrThrow().copy());
                slotTag.putLong("Amount", storedAmounts[i]);
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