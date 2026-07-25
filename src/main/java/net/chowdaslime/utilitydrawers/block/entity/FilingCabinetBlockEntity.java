package net.chowdaslime.utilitydrawers.block.entity;

import net.chowdaslime.utilitydrawers.UtilityDrawersConfig;
import net.chowdaslime.utilitydrawers.block.FilingCabinetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Locale;

import static net.minecraft.sounds.SoundSource.BLOCKS;

public class FilingCabinetBlockEntity extends BlockEntity implements Container {

    private final NonNullList<ItemStack> items = NonNullList.withSize(UtilityDrawersConfig.FILING_CABINET_CAPACITY.get(), ItemStack.EMPTY);

    private int openCount = 0;
    public float openProgress = 0f;
    public float previousOpenProgress = 0f;

    private CompoundTag dropBuffer = null;

    public FilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILING_CABINET_BLOCK_ENTITY.get(), pos, state);
    }

    public enum Category {
        ARMOR, TOOLS, WEAPONS, ENCHANTED_BOOKS, MISC;

        public static Category of(ItemStack stack) {
            if (isArmor(stack)) return ARMOR;
            if (stack.is(Items.ENCHANTED_BOOK)) return ENCHANTED_BOOKS;
            if (isWeapon(stack)) return WEAPONS;
            if (isTool(stack)) return TOOLS;
            return MISC;
        }

        private static boolean isArmor(ItemStack stack) {
            Equippable eq = stack.get(DataComponents.EQUIPPABLE);
            if (eq == null) return false;
            EquipmentSlot slot = eq.slot();
            return slot == EquipmentSlot.HEAD
                    || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.BODY
                    || slot == EquipmentSlot.LEGS
                    || slot == EquipmentSlot.FEET;
        }

        private static boolean isTool(ItemStack stack) {
            return stack.is(ItemTags.PICKAXES)
                    || stack.is(ItemTags.SHOVELS)
                    || stack.is(ItemTags.HOES)
                    || stack.is(ItemTags.AXES)
                    || stack.is(Items.BRUSH)
                    || stack.is(Items.FISHING_ROD)
                    || stack.is(Items.SHEARS);
        }

        private static boolean isWeapon(ItemStack stack) {
            return stack.is(ItemTags.SWORDS)
                    || stack.is(Items.BOW)
                    || stack.is(Items.CROSSBOW)
                    || stack.is(Items.TRIDENT)
                    || stack.is(Items.MACE)
                    || stack.is(ItemTags.SPEARS);
        }

        public String displayName() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }

    public NonNullList<ItemStack> getItemsView() {
        return items;
    }


    @Override
    public int getContainerSize() {
        return UtilityDrawersConfig.FILING_CABINET_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    private void onOpenCountChanged() {
        if (level == null) return;
        level.blockEvent(getBlockPos(), getBlockState().getBlock(), 1, openCount);
    }

    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            openCount = type;
            return true;
        }
        return false;
    }

    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            if (openCount < 0) openCount = 0;
            openCount++;
            onOpenCountChanged();
            if (openCount == 1 && level != null) {
                level.setBlock(getBlockPos(), getBlockState().setValue(FilingCabinetBlock.OPEN, true), 3);
                level.playSound(null, getBlockPos(), SoundEvents.COPPER_TRAPDOOR_OPEN, BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
            }
        }
    }

    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            openCount--;
            onOpenCountChanged();
            if (openCount == 0 && level != null) {
                level.setBlock(getBlockPos(), getBlockState().setValue(FilingCabinetBlock.OPEN, false), 3);
                level.playSound(null, getBlockPos(), SoundEvents.COPPER_TRAPDOOR_CLOSE, BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
            }
        }
    }

    public boolean isOpen() {
        return openCount > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        be.tickAnimation();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FilingCabinetBlockEntity be) {
        be.tickAnimation();
    }

    private void tickAnimation() {
        previousOpenProgress = openProgress;
        float target = isOpen() ? 1f : 0f;
        float speed = 0.1f;

        if (openProgress != target) {
            float old = openProgress;
            if (target > openProgress) {
                openProgress = Math.min(openProgress + speed, target);
            } else {
                openProgress = Math.max(openProgress - speed, target);
            }
            if ((old < 0.5f && openProgress >= 0.5f)) {
            }
            if ((old > 0f && openProgress <= 0f) || (old < 1f && openProgress >= 1f)) {
            }
        }
    }


    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        ContainerHelper.saveAllItems(output, items);
        return output.buildResult();
    }

    public void loadFromItem(CompoundTag tag, HolderLookup.Provider provider) {
        this.items.clear();
        tag.getList("Items").ifPresent(listTag -> {
            for (int i = 0; i < listTag.size(); i++) {
                listTag.getCompound(i).ifPresent(itemTag -> {
                    int slot = itemTag.getShort("Slot").orElse((short) -1) & 0xFFFF;
                    if (slot >= 0 && slot < this.items.size()) {
                        ItemStack stack = itemTag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                        this.items.set(slot, stack);
                    }
                });
            }
        });
        this.setChanged();
    }

    public CompoundTag saveToItem(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putShort("Slot", (short) i);
                itemTag.put("Item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow().copy());
                listTag.add(itemTag);
            }
        }
        if (!listTag.isEmpty()) {
            tag.put("Items", listTag);
        }
        return tag;
    }

    public void prepareForDrop(HolderLookup.Provider provider) {
        this.dropBuffer = this.saveToItem(provider);
        this.items.clear();
    }

    public CompoundTag getDropBuffer() {
        return this.dropBuffer != null ? this.dropBuffer : new CompoundTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class ItemHandler extends SnapshotJournal<ItemHandler.Snapshot> implements ResourceHandler<ItemResource> {

        record Snapshot(ItemStack[] items) {}

        @Override
        public int size() {
            return items.size();
        }

        @Override
        public ItemResource getResource(int slot) {
            return ItemResource.of(items.get(slot));
        }

        @Override
        public long getAmountAsLong(int slot) {
            return items.get(slot).getCount();
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            ItemStack stack = items.get(slot);
            return stack.isEmpty() ? getMaxStackSize() : stack.getMaxStackSize();
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return !resource.isEmpty();
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            updateSnapshots(tx);

            ItemStack incoming = resource.toStack(amount);

            for (int i = 0; i < items.size(); i++) {
                ItemStack existing = items.get(i);

                if (!existing.isEmpty() &&
                        ItemStack.isSameItemSameComponents(existing, incoming)) {

                    int space = existing.getMaxStackSize() - existing.getCount();

                    if (space > 0) {
                        int inserted = Math.min(space, amount);
                        existing.grow(inserted);
                        return inserted;
                    }
                }
            }

            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isEmpty()) {
                    int inserted = Math.min(amount, incoming.getMaxStackSize());
                    items.set(i, incoming.copyWithCount(inserted));
                    return inserted;
                }
            }

            return 0;
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            ItemStack existing = items.get(slot);

            if (existing.isEmpty()) {
                return 0;
            }

            if (!ItemStack.isSameItemSameComponents(existing, resource.toStack(1))) {
                return 0;
            }

            updateSnapshots(tx);

            int extracted = Math.min(existing.getCount(), amount);
            existing.shrink(extracted);

            if (existing.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }

            return extracted;
        }

        @Override
        protected Snapshot createSnapshot() {
            ItemStack[] copy = new ItemStack[items.size()];

            for (int i = 0; i < items.size(); i++) {
                copy[i] = items.get(i).copy();
            }

            return new Snapshot(copy);
        }

        @Override
        protected void revertToSnapshot(Snapshot snapshot) {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, snapshot.items()[i].copy());
            }
        }

        @Override
        protected void onRootCommit(Snapshot snapshot) {
            setChanged();

            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    public ResourceHandler<ItemResource> createItemHandler() {
        return new ItemHandler();
    }
}