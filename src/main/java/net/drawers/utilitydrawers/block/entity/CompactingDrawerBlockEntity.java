package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Optional;

public class CompactingDrawerBlockEntity extends BlockEntity {

    public static final int SLOT_BLOCK  = 0;
    public static final int SLOT_MID    = 1;
    public static final int SLOT_BASE   = 2;

    protected ItemStack baseItem   = ItemStack.EMPTY;
    protected ItemStack midItem    = ItemStack.EMPTY;
    protected ItemStack blockItem  = ItemStack.EMPTY;
    protected int ratio0 = 9;
    protected int ratio1 = 9;
    protected long rawCount = 0L;
    protected long maxRawCapacity = 0L;
    protected boolean locked = false;
    protected BlockPos connectedInterface;

    protected final ItemStack[] upgradeSlots = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    public CompactingDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPACTING_DRAWER_BLOCK_ENTITY.get(), pos, state);
        recalculateCapacity();
    }

    protected CompactingDrawerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        recalculateCapacity();
    }

    public int getSlotCount() {
        return 3;
    }

    public boolean hasInterface() {
        return connectedInterface != null;
    }
    public BlockPos getConnectedInterface() {
        return connectedInterface;
    }
    public void setConnectedInterface(BlockPos pos) {
        connectedInterface = pos; setChanged();
    }
    public void clearConnectedInterface() {
        connectedInterface = null; setChanged();
    }

    public void unlinkFromInterfaces() {
        if (level == null || level.isClientSide() || connectedInterface == null) return;
        if (level.getBlockEntity(connectedInterface) instanceof StorageInterfaceBlockEntity storage) {
            storage.tryUnlinkDrawer(worldPosition);
        }
        connectedInterface = null;
    }

    public ItemStack getUpgradeSlot(int slot)  { return upgradeSlots[slot]; }

    public int getUpgradeMultiplier() {
        int m = 1;
        for (ItemStack u : upgradeSlots) {
            if (!u.isEmpty() && u.getItem() instanceof DrawerUpgradeItem ui) m *= ui.getMultiplier();
        }
        return m;
    }

    public boolean hasVoidUpgrade() {
        for (ItemStack u : upgradeSlots)
            if (u.getItem() instanceof VoidUpgradeItem) return true;
        return false;
    }

    public boolean insertUpgrade(ItemStack upgrade) {
        for (int i = 0; i < 4; i++) {
            if (upgradeSlots[i].isEmpty()) {
                upgradeSlots[i] = upgrade.copyWithCount(1);
                recalculateCapacity();
                setChanged();
                syncToClients();
                return true;
            }
        }
        return false;
    }

    public boolean canRemoveUpgrade(int upgradeSlot) {
        if (upgradeSlots[upgradeSlot].isEmpty()) return false;
        int newMult = 1;
        for (int i = 0; i < 4; i++) {
            if (i == upgradeSlot) continue;
            if (!upgradeSlots[i].isEmpty() && upgradeSlots[i].getItem() instanceof DrawerUpgradeItem u)
                newMult *= u.getMultiplier();
        }
        long newCap = (long) getBaseCapacityInRawUnits() * newMult;
        return rawCount <= newCap;
    }

    public void setUpgradeSlot(int slot, ItemStack stack) {
        upgradeSlots[slot] = stack;
        recalculateCapacity();
        setChanged();
        syncToClients();
    }

    private long getBaseCapacityInRawUnits() {
        return 10L * 64 * ratio0 * ratio1;
    }

    private void recalculateCapacity() {
        maxRawCapacity = getBaseCapacityInRawUnits() * getUpgradeMultiplier();
    }

    public long getBlockCount()  { return rawCount / ((long) ratio0 * ratio1); }

    public long getMidCount()    { return rawCount / ratio0; }

    public long getBaseCount()   { return rawCount; }

    public long getStoredCount(int slot) {
        return switch (slot) {
            case SLOT_BLOCK -> getBlockCount();
            case SLOT_MID   -> getMidCount();
            case SLOT_BASE  -> getBaseCount();
            default -> 0;
        };
    }

    public ItemStack getStoredItem(int slot) {
        return switch (slot) {
            case SLOT_BLOCK -> blockItem;
            case SLOT_MID   -> midItem;
            case SLOT_BASE  -> baseItem;
            default -> ItemStack.EMPTY;
        };
    }

    public boolean isSlotEmpty(int slot) {
        return getStoredItem(slot).isEmpty() || getStoredCount(slot) <= 0;
    }

    public long getMaxCapacity(int slot) {
        return switch (slot) {
            case SLOT_BLOCK -> maxRawCapacity / ((long) ratio0 * ratio1);
            case SLOT_MID   -> maxRawCapacity / ratio0;
            case SLOT_BASE  -> maxRawCapacity;
            default -> 0;
        };
    }

    public boolean isLocked() { return locked; }

    public void setLocked(boolean locked) {
        this.locked = locked;
        if (!locked && rawCount <= 0) {
            clearItems();
        }
        setChanged();
        syncToClients();
    }

    public boolean isFramed() {
        return this instanceof IFramedBlockEntity;
    }

    public IFramedBlockEntity getFramedData() {
        return this instanceof IFramedBlockEntity framed ? framed : null;
    }

    private void clearItems() {
        baseItem  = ItemStack.EMPTY;
        midItem   = ItemStack.EMPTY;
        blockItem = ItemStack.EMPTY;
        rawCount  = 0;
        ratio0 = 9;
        ratio1 = 9;
        recalculateCapacity();
    }

    private record CompressResult(ItemStack output, int ratio) {}
    private record DecompressResult(ItemStack output, int ratio) {}

    private void discoverChainFromAnyTier(ItemStack seed) {
        if (level == null || level.isClientSide()) return;

        ItemStack current = seed.copyWithCount(1);
        int detectedRatio0 = 9;
        int detectedRatio1 = 9;
        ItemStack detectedBase  = current.copyWithCount(1);
        ItemStack detectedMid   = ItemStack.EMPTY;
        ItemStack detectedBlock = ItemStack.EMPTY;

        Optional<DecompressResult> firstDecompress = findDecompressRecipe(current);
        if (firstDecompress.isPresent()) {
            ItemStack oneDown = firstDecompress.get().output().copyWithCount(1);
            int ratio = firstDecompress.get().ratio();

            Optional<DecompressResult> secondDecompress = findDecompressRecipe(oneDown);
            if (secondDecompress.isPresent()) {
                detectedBase  = secondDecompress.get().output().copyWithCount(1);
                detectedMid   = oneDown;
                detectedBlock = current;
                detectedRatio0 = secondDecompress.get().ratio();
                detectedRatio1 = ratio;
            } else {
                detectedBase  = oneDown;
                detectedMid   = current;
                detectedRatio0 = ratio;

                Optional<CompressResult> compress = findCompressRecipe(current);
                if (compress.isPresent()) {
                    detectedBlock  = compress.get().output().copyWithCount(1);
                    detectedRatio1 = compress.get().ratio();
                }
            }
        } else {
            Optional<CompressResult> firstCompress = findCompressRecipe(current);
            if (firstCompress.isPresent()) {
                detectedMid   = firstCompress.get().output().copyWithCount(1);
                detectedRatio0 = firstCompress.get().ratio();

                Optional<CompressResult> secondCompress = findCompressRecipe(detectedMid);
                if (secondCompress.isPresent()) {
                    detectedBlock  = secondCompress.get().output().copyWithCount(1);
                    detectedRatio1 = secondCompress.get().ratio();
                }
            }
        }

        baseItem  = detectedBase;
        midItem   = detectedMid;
        blockItem = detectedBlock;
        ratio0    = detectedRatio0;
        ratio1    = detectedRatio1;
        recalculateCapacity();
    }

    private int getSlotForItem(ItemStack stack) {
        if (!baseItem.isEmpty()  && ItemStack.isSameItemSameComponents(baseItem,  stack)) return SLOT_BASE;
        if (!midItem.isEmpty()   && ItemStack.isSameItemSameComponents(midItem,   stack)) return SLOT_MID;
        if (!blockItem.isEmpty() && ItemStack.isSameItemSameComponents(blockItem, stack)) return SLOT_BLOCK;
        return -1;
    }

    private Optional<CompressResult> findCompressRecipe(ItemStack input) {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();

        var recipeAccess = serverLevel.recipeAccess();

        for (int ratio : new int[]{9, 4}) {
            CraftingInput craftingInput = buildUniformGrid(input, ratio);

            Optional<RecipeHolder<CraftingRecipe>> match =
                    recipeAccess.getRecipeFor(RecipeType.CRAFTING, craftingInput, serverLevel);

            if (match.isPresent()) {
                ItemStack result = match.get().value().assemble(craftingInput);

                if (!result.isEmpty() && result.getCount() == 1
                        && !ItemStack.isSameItemSameComponents(result, input)) {
                    return Optional.of(new CompressResult(result, ratio));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<DecompressResult> findDecompressRecipe(ItemStack input) {
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();

        var recipeAccess = serverLevel.recipeAccess();

        CraftingInput craftingInput = CraftingInput.of(1, 1, List.of(input.copyWithCount(1)));
        Optional<RecipeHolder<CraftingRecipe>> match =
                recipeAccess.getRecipeFor(RecipeType.CRAFTING, craftingInput, serverLevel);

        if (match.isPresent()) {
            ItemStack result = match.get().value().assemble(craftingInput);
            if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, input)) {
                int count = result.getCount();
                if (count == 9 || count == 4) {
                    return Optional.of(new DecompressResult(result.copyWithCount(1), count));
                }
            }
        }
        return Optional.empty();
    }

    private CraftingInput buildUniformGrid(ItemStack item, int count) {
        int side = (count == 9) ? 3 : 2;
        List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) items.add(item.copyWithCount(1));
        return CraftingInput.of(side, side, items);
    }

    public ItemStack insertItemIntoSlot(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getItem() instanceof StorageRemoteItem) return stack;

        if (baseItem.isEmpty()) {
            if (locked) return stack;
            if (!simulate) discoverChainFromAnyTier(stack);
        }

        int resolvedSlot = getSlotForItem(stack);
        if (resolvedSlot == -1) {
            return stack;
        }

        long rawPerUnit = rawUnitsPerSlot(resolvedSlot);
        long spaceRaw   = maxRawCapacity - rawCount;
        long spaceUnits = spaceRaw / rawPerUnit;

        if (spaceUnits <= 0) {
            return hasVoidUpgrade() ? ItemStack.EMPTY : stack;
        }

        int toInsert  = (int) Math.min(stack.getCount(), spaceUnits);
        int remainder = stack.getCount() - toInsert;

        if (!simulate) {
            rawCount += (long) toInsert * rawPerUnit;
            setChanged();
            syncToClients();
        }

        if (remainder > 0 && hasVoidUpgrade()) return ItemStack.EMPTY;
        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack slotItem = getStoredItem(slot);
        if (slotItem.isEmpty()) return ItemStack.EMPTY;

        long available = getStoredCount(slot);
        if (available <= 0) return ItemStack.EMPTY;

        int maxStack = slotItem.getMaxStackSize();
        int toExtract = (int) Math.min(amount, Math.min(available, maxStack));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            rawCount -= (long) toExtract * rawUnitsPerSlot(slot);
            if (rawCount < 0) rawCount = 0;
            if (rawCount == 0 && !locked) clearItems();
            setChanged();
            syncToClients();
        }

        return slotItem.copyWithCount(toExtract);
    }

    private long rawUnitsPerSlot(int slot) {
        return switch (slot) {
            case SLOT_BLOCK -> (long) ratio0 * ratio1;
            case SLOT_MID   -> ratio0;
            case SLOT_BASE  -> 1L;
            default -> 1L;
        };
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Locked", locked);
        output.putLong("RawCount", rawCount);
        output.putInt("Ratio0", ratio0);
        output.putInt("Ratio1", ratio1);

        if (connectedInterface != null)
            output.putLong("ConnectedInterface", connectedInterface.asLong());

        if (!baseItem.isEmpty())  output.store("BaseItem",  ItemStack.CODEC, baseItem);
        if (!midItem.isEmpty())   output.store("MidItem",   ItemStack.CODEC, midItem);
        if (!blockItem.isEmpty()) output.store("BlockItem", ItemStack.CODEC, blockItem);

        for (int i = 0; i < 4; i++) {
            if (!upgradeSlots[i].isEmpty()) {
                output.child("Upgrade" + i).store("Item", ItemStack.CODEC, upgradeSlots[i]);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        locked   = input.getBooleanOr("Locked", false);
        rawCount = input.getLongOr("RawCount", 0L);
        ratio0   = input.getIntOr("Ratio0", 9);
        ratio1   = input.getIntOr("Ratio1", 9);

        long ifacePos = input.getLongOr("ConnectedInterface", Long.MIN_VALUE);
        connectedInterface = (ifacePos != Long.MIN_VALUE) ? BlockPos.of(ifacePos) : null;

        baseItem  = input.read("BaseItem",  ItemStack.CODEC).orElse(ItemStack.EMPTY);
        midItem   = input.read("MidItem",   ItemStack.CODEC).orElse(ItemStack.EMPTY);
        blockItem = input.read("BlockItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            upgradeSlots[idx] = ItemStack.EMPTY;
            input.child("Upgrade" + idx).ifPresent(u ->
                    upgradeSlots[idx] = u.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        recalculateCapacity();
    }

    public CompoundTag saveDrawerData(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Locked", locked);
        tag.putLong("RawCount", rawCount);
        tag.putInt("Ratio0", ratio0);
        tag.putInt("Ratio1", ratio1);

        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        if (!baseItem.isEmpty())
            tag.put("BaseItem",  ItemStack.CODEC.encodeStart(ops, baseItem).getOrThrow().copy());
        if (!midItem.isEmpty())
            tag.put("MidItem",   ItemStack.CODEC.encodeStart(ops, midItem).getOrThrow().copy());
        if (!blockItem.isEmpty())
            tag.put("BlockItem", ItemStack.CODEC.encodeStart(ops, blockItem).getOrThrow().copy());

        for (int i = 0; i < 4; i++) {
            if (!upgradeSlots[i].isEmpty()) {
                CompoundTag u = new CompoundTag();
                u.put("Item", ItemStack.CODEC.encodeStart(ops, upgradeSlots[i]).getOrThrow().copy());
                tag.put("Upgrade" + i, u);
            }
        }
        return tag;
    }

    public void loadContentsFromTag(CompoundTag tag) {
        locked   = tag.getBooleanOr("Locked", false);
        rawCount = tag.getLongOr("RawCount", 0L);
        ratio0   = tag.getIntOr("Ratio0", 9);
        ratio1   = tag.getIntOr("Ratio1", 9);

        if (level != null) {
            var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            baseItem  = tag.contains("BaseItem")
                    ? ItemStack.CODEC.parse(ops, tag.get("BaseItem")).resultOrPartial().orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            midItem   = tag.contains("MidItem")
                    ? ItemStack.CODEC.parse(ops, tag.get("MidItem")).resultOrPartial().orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            blockItem = tag.contains("BlockItem")
                    ? ItemStack.CODEC.parse(ops, tag.get("BlockItem")).resultOrPartial().orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;

            for (int i = 0; i < 4; i++) {
                upgradeSlots[i] = ItemStack.EMPTY;
                final int idx = i;
                tag.getCompound("Upgrade" + i).ifPresent(u -> {
                    upgradeSlots[idx] = ItemStack.CODEC.parse(ops, u.get("Item"))
                            .resultOrPartial().orElse(ItemStack.EMPTY);
                });
            }
        }
        recalculateCapacity();
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveDrawerData(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        syncToClients();
    }

    private final class ItemHandler
            extends SnapshotJournal<ItemHandler.Snapshot>
            implements ResourceHandler<ItemResource> {

        record Snapshot(long rawCount, ItemStack base, ItemStack mid, ItemStack block) {}

        @Override public int size() { return 3; }

        @Override
        public ItemResource getResource(int slot) {
            return ItemResource.of(getStoredItem(slot));
        }

        @Override
        public long getAmountAsLong(int slot) {
            return getStoredCount(slot);
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            return getMaxCapacity(slot);
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            if (resource.isEmpty()) return false;
            if (!baseItem.isEmpty()  && ItemResource.of(baseItem).equals(resource))  return true;
            if (!midItem.isEmpty()   && ItemResource.of(midItem).equals(resource))   return true;
            if (!blockItem.isEmpty() && ItemResource.of(blockItem).equals(resource)) return true;
            return baseItem.isEmpty() && !locked;
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            updateSnapshots(tx);
            ItemStack remainder = insertItemIntoSlot(slot, resource.toStack(amount), false);
            return amount - remainder.getCount();
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            if (!ItemResource.of(getStoredItem(slot)).equals(resource)) return 0;
            updateSnapshots(tx);
            return extractItem(slot, amount, false).getCount();
        }

        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(rawCount, baseItem.copy(), midItem.copy(), blockItem.copy());
        }

        @Override
        protected void revertToSnapshot(Snapshot s) {
            rawCount  = s.rawCount();
            baseItem  = s.base().copy();
            midItem   = s.mid().copy();
            blockItem = s.block().copy();
        }

        @Override
        protected void onRootCommit(Snapshot originalState) {
            setChanged();
            syncToClients();
        }
    }

    public ResourceHandler<ItemResource> createItemHandler() {
        return new ItemHandler();
    }
}