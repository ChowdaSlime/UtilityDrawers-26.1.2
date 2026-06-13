package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.block.DrawerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;


public class DrawerBlockEntity extends BlockEntity {

    private static final long DEFAULT_MAX_CAPACITY = 64L * 32L;

    private final int slotCount;
    private final ItemStack[] storedStacks;
    private final long[] storedCounts;
    private final long[] maxCapacities;

    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWER_BLOCK_ENTITY.get(), pos, state);
        this.slotCount = (state.getBlock() instanceof DrawerBlock provider) ? provider.getSlotCount() : 1;
        this.storedStacks = new ItemStack[slotCount];
        this.storedCounts = new long[slotCount];
        this.maxCapacities = new long[slotCount];
        for (int i = 0; i < slotCount; i++) {
            storedStacks[i] = ItemStack.EMPTY;
            storedCounts[i] = 0L;
            maxCapacities[i] = DEFAULT_MAX_CAPACITY;
        }
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

    public long getMaxCapacity(int slot) {
        return maxCapacities[slot];
    }

    public boolean isSlotEmpty(int slot) {
        return storedStacks[slot].isEmpty() || storedCounts[slot] <= 0;
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        for (int i = 0; i < slotCount; i++) {
            if (!storedStacks[i].isEmpty() && ItemStack.isSameItemSameComponents(storedStacks[i], stack)) {
                return insertIntoSlot(i, stack, simulate);
            }
        }

        for (int i = 0; i < slotCount; i++) {
            if (storedStacks[i].isEmpty()) {
                return insertIntoSlot(i, stack, simulate);
            }
        }

        return stack;
    }

    private ItemStack insertIntoSlot(int slot, ItemStack stack, boolean simulate) {
        long space = maxCapacities[slot] - storedCounts[slot];
        if (space <= 0) return stack;

        int toInsert = (int) Math.min(stack.getCount(), space);

        if (!simulate) {
            if (storedStacks[slot].isEmpty()) {
                storedStacks[slot] = stack.copyWithCount(1);
            }
            storedCounts[slot] += toInsert;
            setChanged();
        }

        int remainder = stack.getCount() - toInsert;
        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slotCount) return ItemStack.EMPTY;
        if (storedStacks[slot].isEmpty() || storedCounts[slot] <= 0) return ItemStack.EMPTY;

        int maxStackSize = storedStacks[slot].getMaxStackSize();
        int toExtract = (int) Math.min(amount, Math.min(storedCounts[slot], maxStackSize));

        if (toExtract <= 0) return ItemStack.EMPTY;

        ItemStack result = storedStacks[slot].copyWithCount(toExtract);

        if (!simulate) {
            storedCounts[slot] -= toExtract;
            if (storedCounts[slot] <= 0) {
                storedStacks[slot] = ItemStack.EMPTY;
                storedCounts[slot] = 0;
            }
            setChanged();
        }

        return result;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < slotCount; i++) {
            ValueOutput slotOutput = output.child("Slot" + i);
            if (!storedStacks[i].isEmpty()) {
                slotOutput.store("Item", ItemStack.CODEC, storedStacks[i]);
                slotOutput.putLong("Count", storedCounts[i]);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < slotCount; i++) {
            final int slot = i;
            storedStacks[slot] = ItemStack.EMPTY;
            storedCounts[slot] = 0;
            input.child("Slot" + slot).ifPresent(slotInput -> {
                storedStacks[slot] = slotInput.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                storedCounts[slot] = slotInput.getLongOr("Count", 0L);
            });
        }
    }



    public void loadContentsFromTag(CompoundTag tag) {
        for (int i = 0; i < slotCount; i++) {
            storedStacks[i] = ItemStack.EMPTY;
            storedCounts[i] = 0L;

            if (tag.getCompound("Slot" + i).isPresent()) {
                CompoundTag slotTag = tag.getCompound("Slot" + i).orElseThrow();
                storedStacks[i] = slotTag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                storedCounts[i] = slotTag.getLongOr("Count", 0L);
            }
        }

        setChanged();
    }

    public CompoundTag saveDrawerData(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < slotCount; i++) {
            if (!storedStacks[i].isEmpty() && storedCounts[i] > 0) {
                CompoundTag slotTag = new CompoundTag();
                // Use the provider to save the ItemStack using the new Codec system
                slotTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), storedStacks[i]).getOrThrow().copy());
                slotTag.putLong("Count", storedCounts[i]);
                tag.put("Slot" + i, slotTag);
            }
        }
        return tag;
    }

}