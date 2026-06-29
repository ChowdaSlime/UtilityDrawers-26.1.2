package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

public class StorageViewerExtractHandler {

    public static void handle(StorageViewerExtractPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof StorageViewerMenu menu)) return;

            Level level = player.level();
            ItemStack template = payload.stack();
            int requestedAmount = payload.amount();
            StorageViewerExtractPayload.ExtractMode mode = payload.mode();

            int toExtract = switch (mode) {
                case CURSOR_FULL -> Math.min(requestedAmount, template.getMaxStackSize());
                case CURSOR_HALF -> Math.max(1, Math.min(requestedAmount / 2, template.getMaxStackSize() / 2));
                case SHIFT_MOVE -> Math.min(requestedAmount, template.getMaxStackSize());
            };

            ItemStack extracted = ItemStack.EMPTY;
            int remaining = toExtract;

            outer:
            for (StorageViewerMenu.DrawerSlotRef ref : payload.sources()) {
                BlockEntity be = level.getBlockEntity(ref.pos());

                if (be instanceof DrawerBlockEntity drawer) {
                    int slot = ref.slotIndex();
                    ItemStack stored = drawer.getStoredItem(slot);
                    if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, template)) continue;

                    long available = drawer.getStoredCount(slot);
                    int take = (int) Math.min(remaining, available);
                    if (take <= 0) continue;

                    ItemStack taken = drawer.extractItem(slot, take, false);
                    if (!taken.isEmpty()) {
                        if (extracted.isEmpty()) {
                            extracted = taken;
                        } else {
                            extracted.grow(taken.getCount());
                        }
                        remaining -= taken.getCount();
                    }

                } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                    int slot = ref.slotIndex();
                    ItemStack stored = compacting.getStoredItem(slot);
                    if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, template)) continue;

                    long available = compacting.getStoredCount(slot);
                    int take = (int) Math.min(remaining, available);
                    if (take <= 0) continue;

                    ItemStack taken = compacting.extractItem(slot, take, false);
                    if (!taken.isEmpty()) {
                        if (extracted.isEmpty()) {
                            extracted = taken;
                        } else {
                            extracted.grow(taken.getCount());
                        }
                        remaining -= taken.getCount();
                    }
                } else if (be instanceof FluidDrawerBlockEntity fluidDrawer) {
                    int slot = ref.slotIndex();
                    FluidStack stored = fluidDrawer.getStoredFluid(slot);
                    if (stored.isEmpty()) continue;

                    Item bucketItem = stored.getFluid().getBucket();
                    if (bucketItem == Items.AIR) continue;
                    ItemStack bucketStack = new ItemStack(bucketItem);
                    if (!ItemStack.isSameItemSameComponents(bucketStack, template)) continue;

                    int bucketsToExtract = remaining;
                    int mbToExtract = bucketsToExtract * 1000;

                    FluidStack taken =
                            fluidDrawer.extractFluid(slot, mbToExtract, false);

                    if (taken.isEmpty()) continue;

                    int bucketsExtracted = taken.getAmount() / 1000;
                    if (bucketsExtracted <= 0) {
                        fluidDrawer.insertFluidIntoSlot(slot, taken, false);
                        continue;
                    }

                    int bucketsAvailable = 0;
                    ItemStack emptyBucket = new ItemStack(Items.BUCKET);

                    ItemStack cursor = player.containerMenu.getCarried();
                    if (ItemStack.isSameItemSameComponents(cursor, emptyBucket)) {
                        bucketsAvailable += cursor.getCount();
                    }

                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invSlot = player.getInventory().getItem(i);
                        if (ItemStack.isSameItemSameComponents(invSlot, emptyBucket)) {
                            bucketsAvailable += invSlot.getCount();
                        }
                    }

                    int networkBuckets = 0;
                    int deficit = bucketsExtracted - bucketsAvailable;
                    if (deficit > 0) {
                        networkBuckets = pullItemsFromNetwork(menu, level, emptyBucket, deficit);
                        bucketsAvailable += networkBuckets;
                    }

                    if (bucketsAvailable < bucketsExtracted) {
                        bucketsExtracted = bucketsAvailable;
                    }

                    if (bucketsExtracted <= 0) {
                        fluidDrawer.insertFluidIntoSlot(slot, taken, false);
                        continue;
                    }

                    int toConsume = bucketsExtracted - networkBuckets;
                    if (ItemStack.isSameItemSameComponents(cursor, emptyBucket) && toConsume > 0) {
                        int consume = Math.min(toConsume, cursor.getCount());
                        cursor.shrink(consume);
                        toConsume -= consume;
                        player.containerMenu.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                    }
                    for (int i = 0; i < player.getInventory().getContainerSize() && toConsume > 0; i++) {
                        ItemStack invSlot = player.getInventory().getItem(i);
                        if (ItemStack.isSameItemSameComponents(invSlot, emptyBucket)) {
                            int consume = Math.min(toConsume, invSlot.getCount());
                            invSlot.shrink(consume);
                            toConsume -= consume;
                            player.getInventory().setItem(i, invSlot.isEmpty() ? ItemStack.EMPTY : invSlot);
                        }
                    }
                    player.getInventory().setChanged();

                    int usedMb = bucketsExtracted * 1000;
                    int leftoverMb = taken.getAmount() - usedMb;
                    if (leftoverMb > 0) {
                        fluidDrawer.insertFluidIntoSlot(slot,
                                taken.copyWithAmount(leftoverMb), false);
                    }

                    ItemStack buckets = new ItemStack(bucketItem, bucketsExtracted);
                    if (extracted.isEmpty()) {
                        extracted = buckets;
                    } else {
                        extracted.grow(buckets.getCount());
                    }
                    remaining -= bucketsExtracted;
                }

                if (remaining <= 0) break;
            }

            if (extracted.isEmpty()) return;


            if (mode == StorageViewerExtractPayload.ExtractMode.SHIFT_MOVE) {
                ItemStack leftover = extracted.copy();
                leftover = addToPlayerInventory(player, leftover);
                if (!leftover.isEmpty()) {
                    menu.insertIntoNetwork(leftover, player);
                }
            } else {
                ItemStack cursor = player.containerMenu.getCarried();
                if (cursor.isEmpty()) {
                    player.containerMenu.setCarried(extracted);
                } else if (ItemStack.isSameItemSameComponents(cursor, extracted)) {
                    int canAdd = cursor.getMaxStackSize() - cursor.getCount();
                    int add = Math.min(canAdd, extracted.getCount());
                    cursor.grow(add);
                    int leftover = extracted.getCount() - add;
                    if (leftover > 0) {
                        menu.insertIntoNetwork(extracted.copyWithCount(leftover), player);
                    }
                    player.containerMenu.setCarried(cursor);
                } else {
                    ItemStack leftover = addToPlayerInventory(player, extracted);
                    if (!leftover.isEmpty()) {
                        player.drop(leftover, false);
                    }
                }
            }

            menu.refreshNetworkSlots();
            PacketDistributor.sendToPlayer(player,
                    new SyncNetworkSlotsPayload(menu.networkSlots));
        });
    }

    private static ItemStack addToPlayerInventory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        for (int i = 0; i < player.getInventory().getContainerSize() && !remainder.isEmpty(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(slot, remainder)) {
                int canAdd = slot.getMaxStackSize() - slot.getCount();
                int add = Math.min(canAdd, remainder.getCount());
                slot.grow(add);
                remainder.shrink(add);
                player.getInventory().setItem(i, slot);
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize() && !remainder.isEmpty(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, remainder.copy());
                remainder = ItemStack.EMPTY;
            }
        }
        player.getInventory().setChanged();
        return remainder;
    }

    private static int pullItemsFromNetwork(StorageViewerMenu menu, Level level, ItemStack template, int amount) {
        int extracted = 0;
        for (StorageViewerMenu.NetworkSlot ns : menu.networkSlots) {
            if (ItemStack.isSameItemSameComponents(ns.stack(), template)) {
                for (StorageViewerMenu.DrawerSlotRef ref : ns.sources()) {
                    if (extracted >= amount) break;

                    BlockEntity be = level.getBlockEntity(ref.pos());
                    if (be instanceof DrawerBlockEntity drawer) {
                        int slot = ref.slotIndex();
                        ItemStack stored = drawer.getStoredItem(slot);
                        if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, template)) {
                            int take = (int) Math.min(amount - extracted, drawer.getStoredCount(slot));
                            if (take > 0) {
                                ItemStack taken = drawer.extractItem(slot, take, false);
                                extracted += taken.getCount();
                            }
                        }
                    } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                        int slot = ref.slotIndex();
                        ItemStack stored = compacting.getStoredItem(slot);
                        if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, template)) {
                            int take = (int) Math.min(amount - extracted, compacting.getStoredCount(slot));
                            if (take > 0) {
                                ItemStack taken = compacting.extractItem(slot, take, false);
                                extracted += taken.getCount();
                            }
                        }
                    }
                }
            }
        }
        return extracted;
    }
}