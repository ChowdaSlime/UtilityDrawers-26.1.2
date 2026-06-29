package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.attachment.ModAttachments;
import net.drawers.utilitydrawers.attachment.PlayerPreferences;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.network.SyncNetworkSlotsPayload;
import net.drawers.utilitydrawers.network.SyncPreferencesPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class StorageViewerMenu extends AbstractContainerMenu {

    private final StorageInterfaceBlockEntity storageInterface;
    private final BlockPos viewerPos;
    private final Player player;
    private int syncTimer = 0;
    public boolean clientNeedsRebuild = false;
    public boolean sortByCount = false;
    public boolean sortAscending = true;

    public record DrawerSlotRef(BlockPos pos, int slotIndex) {
        public static final StreamCodec<RegistryFriendlyByteBuf, DrawerSlotRef> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, DrawerSlotRef::pos,
                        ByteBufCodecs.VAR_INT, DrawerSlotRef::slotIndex,
                        DrawerSlotRef::new
                );
    }

    public record NetworkSlot(ItemStack stack, long count, List<DrawerSlotRef> sources, boolean isFluid) {
        public static final StreamCodec<RegistryFriendlyByteBuf, NetworkSlot> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC, NetworkSlot::stack,
                        ByteBufCodecs.VAR_LONG, NetworkSlot::count,
                        DrawerSlotRef.STREAM_CODEC.apply(ByteBufCodecs.list()), NetworkSlot::sources,
                        ByteBufCodecs.BOOL, NetworkSlot::isFluid,
                        NetworkSlot::new
                );
    }

    public List<NetworkSlot> networkSlots = new ArrayList<>();

    public StorageViewerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                buf.readBoolean() ? (StorageInterfaceBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()) : null,
                buf.readBlockPos());
    }

    public StorageViewerMenu(int containerId, Inventory playerInventory,
                             StorageInterfaceBlockEntity storageInterface, BlockPos viewerPos) {
        super(ModMenuTypes.STORAGE_VIEWER_MENU.get(), containerId);
        this.storageInterface = storageInterface;
        this.viewerPos = viewerPos;
        this.player = playerInventory.player;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 146));
        }

        if (this.storageInterface != null && this.storageInterface.getLevel() != null
                && !this.storageInterface.getLevel().isClientSide()) {
            refreshNetworkSlots();
            loadPreferences();
        }
    }

    public void refreshNetworkSlots() {
        networkSlots = new ArrayList<>();
        if (storageInterface == null) return;

        Level level = storageInterface.getLevel();
        if (level == null) return;

        List<AggEntry> agg = new ArrayList<>();

        for (BlockPos pos : storageInterface.getConnectedDrawers()) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    ItemStack stored = drawer.getStoredItem(i);
                    if (!stored.isEmpty()) {
                        long count = drawer.getStoredCount(i);
                        mergeIntoAgg(agg, stored, count, pos, i, false);
                    }
                }
            } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                for (int i = 0; i < compacting.getSlotCount(); i++) {
                    ItemStack stored = compacting.getStoredItem(i);
                    if (!stored.isEmpty()) {
                        long count = compacting.getStoredCount(i);
                        mergeIntoAgg(agg, stored, count, pos, i, false);
                    }
                }
            } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    FluidStack fluidStack = fluidDrawer.getStoredFluid(i);
                    if (!fluidStack.isEmpty()) {
                        long fluidCount = fluidDrawer.getStoredAmount(i);
                        Item bucketItem = fluidStack.getFluid().getBucket();
                        if (bucketItem != Items.AIR) {
                            ItemStack renderStack = new ItemStack(bucketItem);
                            mergeIntoAgg(agg, renderStack, fluidCount, pos, i, true);
                        }
                    }
                }
            }
        }

        for (AggEntry entry : agg) {
            networkSlots.add(new NetworkSlot(entry.representative, entry.totalCount, entry.sources, entry.isFluid));
        }
    }

    public void mergeIntoAgg(List<AggEntry> agg, ItemStack stack, long count,
                             BlockPos pos, int slot, boolean isFluid) {
        for (AggEntry entry : agg) {
            if (ItemStack.isSameItemSameComponents(entry.representative, stack)) {
                entry.totalCount += count;
                entry.sources.add(new DrawerSlotRef(pos, slot));
                return;
            }
        }
        AggEntry newEntry = new AggEntry();
        newEntry.representative = stack.copyWithCount(1);
        newEntry.totalCount = count;
        newEntry.sources = new ArrayList<>();
        newEntry.sources.add(new DrawerSlotRef(pos, slot));
        newEntry.isFluid = isFluid;
        agg.add(newEntry);
    }

    public static class AggEntry {
        public ItemStack representative;
        public long totalCount;
        public List<DrawerSlotRef> sources;
        public boolean isFluid;
    }

    public List<NetworkSlot> getNetworkSlots() {
        return networkSlots;
    }

    public void setNetworkSlots(List<NetworkSlot> slots) {
        this.networkSlots = slots;
        this.clientNeedsRebuild = true;
    }

    public StorageInterfaceBlockEntity getStorageInterface() {
        return storageInterface;
    }

    public ItemStack insertIntoNetwork(ItemStack stack, Player player) {
        if (storageInterface == null || stack.isEmpty()) return stack;

        if (storageInterface.getLevel() != null && storageInterface.getLevel().isClientSide()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = storageInterface.insertIntoNetwork(stack);
        refreshNetworkSlots();

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkSlotsPayload(this.networkSlots));
        }

        this.broadcastChanges();
        return remainder;
    }

    public FluidStack insertFluidIntoNetwork(FluidStack fluidStack, Player player) {
        if (storageInterface == null || fluidStack.isEmpty()) return fluidStack;

        if (storageInterface.getLevel() != null && storageInterface.getLevel().isClientSide()) {
            return FluidStack.EMPTY;
        }

        FluidStack remainder = storageInterface.insertFluidIntoNetwork(fluidStack);
        refreshNetworkSlots();

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkSlotsPayload(this.networkSlots));
        }

        this.broadcastChanges();
        return remainder;
    }

    @Override
    public boolean stillValid(Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), viewerPos);
        return stillValid(access, player, ModBlocks.STORAGE_VIEWER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < 36) {
                ItemStack remainder = insertIntoNetwork(slotStack, player);

                if (!player.level().isClientSide() && remainder.getCount() == itemstack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.set(remainder);
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (this.player instanceof ServerPlayer serverPlayer) {
            syncTimer++;

            if (syncTimer == 1) {
                loadPreferences();
                refreshNetworkSlots();
                PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkSlotsPayload(this.networkSlots));
                PacketDistributor.sendToPlayer(serverPlayer, new SyncPreferencesPayload(this.sortByCount, this.sortAscending));
            } else if (syncTimer % 10 == 0) {
                refreshNetworkSlots();
                PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkSlotsPayload(this.networkSlots));
            }
        }
    }

    public void loadPreferences() {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerPreferences prefs = serverPlayer.getData(ModAttachments.PLAYER_PREFERENCES.get());
            this.sortByCount = prefs.isSortByCount();
            this.sortAscending = prefs.isSortAscending();
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPreferencesPayload(this.sortByCount, this.sortAscending));
        }
    }

    public void saveSortPreference(boolean value) {
        this.sortByCount = value;
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerPreferences prefs = serverPlayer.getData(ModAttachments.PLAYER_PREFERENCES.get());
            prefs.setSortByCount(value);
            prefs.setSortAscending(this.sortAscending);
            serverPlayer.setData(ModAttachments.PLAYER_PREFERENCES.get(), prefs);
        }
    }
}