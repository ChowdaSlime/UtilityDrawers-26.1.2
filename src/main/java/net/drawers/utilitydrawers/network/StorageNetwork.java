package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StorageNetwork {

    private List<StorageViewerMenu.NetworkSlot> cachedItems = new ArrayList<>();
    private List<StorageViewerMenu.NetworkSlot> cachedFluids = new ArrayList<>();

    private final Set<BlockPos> nodes = new HashSet<>();

    private boolean dirty = true;

    public void addNode(BlockPos pos) {
        nodes.add(pos);
        dirty = true;
    }

    public void removeNode(BlockPos pos) {
        nodes.remove(pos);
        dirty = true;
    }

    public void markDirty() {
        dirty = true;
    }

    public Set<BlockPos> getNodes() {
        return nodes;
    }

    public List<StorageViewerMenu.NetworkSlot> getItems() {
        return cachedItems;
    }

    public List<StorageViewerMenu.NetworkSlot> getFluids() {
        return cachedFluids;
    }

    public void rebuild(Level level, Set<BlockPos> nodes) {
        cachedItems = new ArrayList<>();
        cachedFluids = new ArrayList<>();

        if (level == null) return;

        List<StorageViewerMenu.AggEntry> agg = new ArrayList<>();

        for (BlockPos pos : nodes) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;

            if (be instanceof DrawerBlockEntity drawer) {
                for (int i = 0; i < drawer.getSlotCount(); i++) {
                    ItemStack stored = drawer.getStoredItem(i);
                    if (!stored.isEmpty()) {
                        long count = drawer.getStoredCount(i);
                        mergeIntoAgg(agg, stored, count, pos, i);
                    }
                }
            }

            else if (be instanceof CompactingDrawerBlockEntity compacting) {
                for (int i = 0; i < compacting.getSlotCount(); i++) {
                    ItemStack stored = compacting.getStoredItem(i);
                    if (!stored.isEmpty()) {
                        long count = compacting.getStoredCount(i);
                        mergeIntoAgg(agg, stored, count, pos, i);
                    }
                }
            }

            else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                for (int i = 0; i < fluidDrawer.getSlotCount(); i++) {
                    FluidStack fluid = fluidDrawer.getStoredFluid(i);
                    if (!fluid.isEmpty()) {
                        long amount = fluid.getAmount();
                        Item bucket = fluid.getFluid().getBucket();

                        if (bucket != Items.AIR) {
                            mergeIntoAgg(agg, new ItemStack(bucket), amount, pos, i);
                        }
                    }
                }
            }
        }

        for (StorageViewerMenu.AggEntry entry : agg) {
            cachedItems.add(
                    new StorageViewerMenu.NetworkSlot(
                            entry.representative,
                            entry.totalCount,
                            entry.sources
                    )
            );
        }
    }

    private void mergeIntoAgg(
            List<StorageViewerMenu.AggEntry> agg,
            ItemStack stack,
            long count,
            BlockPos pos,
            int slot
    ) {
        for (StorageViewerMenu.AggEntry entry : agg) {
            if (ItemStack.isSameItemSameComponents(entry.representative, stack)) {
                entry.totalCount += count;
                entry.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));
                return;
            }
        }

        StorageViewerMenu.AggEntry newEntry = new StorageViewerMenu.AggEntry();
        newEntry.representative = stack.copyWithCount(1);
        newEntry.totalCount = count;
        newEntry.sources = new ArrayList<>();
        newEntry.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));

        agg.add(newEntry);
    }

    private void mergeItem(List<StorageViewerMenu.AggEntry> agg, ItemStack stack, long count, BlockPos pos, int slot) {
        for (StorageViewerMenu.AggEntry e : agg) {
            if (ItemStack.isSameItemSameComponents(e.representative, stack)) {
                e.totalCount += count;
                e.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));
                return;
            }
        }

        StorageViewerMenu.AggEntry e = new StorageViewerMenu.AggEntry();
        e.representative = stack.copyWithCount(1);
        e.totalCount = count;
        e.sources = new ArrayList<>();
        e.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));
        agg.add(e);
    }

    private void mergeFluid(List<StorageViewerMenu.AggEntry> agg, ItemStack stack, long count, BlockPos pos, int slot) {
        for (StorageViewerMenu.AggEntry e : agg) {
            if (ItemStack.isSameItemSameComponents(e.representative, stack)) {
                e.totalCount += count;
                e.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));
                return;
            }
        }

        StorageViewerMenu.AggEntry e = new StorageViewerMenu.AggEntry();
        e.representative = stack.copyWithCount(1);
        e.totalCount = count;
        e.sources = new ArrayList<>();
        e.sources.add(new StorageViewerMenu.DrawerSlotRef(pos, slot));
        agg.add(e);
    }

    private void addItem(List<StorageViewerMenu.NetworkSlot> list, ItemStack stack, long count) {
        for (StorageViewerMenu.NetworkSlot slot : list) {
            if (ItemStack.isSameItemSameComponents(slot.stack(), stack)) {
                slot.sources().addAll(new ArrayList<>());
                return;
            }
        }
    }
}