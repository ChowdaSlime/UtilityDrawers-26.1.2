package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class StorageViewerInsertHandler {

    public static void handle(StorageViewerInsertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof StorageViewerMenu menu)) return;

            ItemStack cursor = player.containerMenu.getCarried();
            if (cursor.isEmpty()) return;

            ItemAccess cursorAccess = ItemAccess.forPlayerCursor(player, player.containerMenu);
            ResourceHandler<FluidResource> itemFluidHandler =
                    cursorAccess.getCapability(Capabilities.Fluid.ITEM);

            boolean isFilledBucket = itemFluidHandler != null
                    && !cursor.is(Items.BUCKET)
                    && itemFluidHandler.size() > 0
                    && !itemFluidHandler.getResource(0).isEmpty();

            if (isFilledBucket && payload.insertAsFluid()) {
                var storageInterface = menu.getStorageInterface();
                if (storageInterface == null) return;

                ResourceHandler<FluidResource> networkFluidHandler =
                        storageInterface.createFluidHandler();

                FluidResource fluidResource = itemFluidHandler.getResource(0);

                try (Transaction tx = Transaction.openRoot()) {
                    int inserted = networkFluidHandler.insert(fluidResource, 1000, tx);
                    if (inserted == 1000) {
                        int drained = itemFluidHandler.extract(0, fluidResource, 1000, tx);
                        if (drained == 1000) {
                            tx.commit();
                            cursor.shrink(1);
                            ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                            if (cursor.isEmpty()) {
                                player.containerMenu.setCarried(emptyBucket);
                            } else {
                                player.containerMenu.setCarried(cursor);
                                player.getInventory().placeItemBackInInventory(emptyBucket);
                            }
                            player.getInventory().setChanged();
                        }
                    }
                }

            } else {
                int insertAmount = Math.min(payload.amount(), cursor.getCount());
                ItemStack toInsert = cursor.copyWithCount(insertAmount);
                ItemStack remainder = menu.insertIntoNetwork(toInsert, player);
                int inserted = insertAmount - remainder.getCount();
                cursor.shrink(inserted);
                player.containerMenu.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
            }

            menu.refreshNetworkSlots();
            PacketDistributor.sendToPlayer(player,
                    new SyncNetworkSlotsPayload(menu.networkSlots));
        });
    }
}