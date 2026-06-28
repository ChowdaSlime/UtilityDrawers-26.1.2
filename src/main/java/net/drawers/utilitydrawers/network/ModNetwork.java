package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.drawers.utilitydrawers.menu.StorageViewerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModNetwork {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(UtilityDrawers.MODID);

        registrar.playToServer(
                CycleRemoteModePayload.TYPE,
                CycleRemoteModePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        ItemStack stack = player.getMainHandItem();

                        if (stack.getItem() instanceof StorageRemoteItem) {
                            StorageRemoteItem.cycleMode(stack);

                            boolean isLink = StorageRemoteItem.isLinkMode(stack);
                            ChatFormatting modeColor = isLink ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE;
                            String modeName = isLink ? "Link/Unlink" : "Lock/Unlock";

                            player.sendOverlayMessage(
                                    Component.literal("Mode: ")
                                            .append(Component.literal(modeName).withStyle(modeColor))
                            );
                        }
                    });
                }
        );

        registrar.playToServer(
                StorageViewerExtractPayload.TYPE,
                StorageViewerExtractPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        Level level = player.level();

                        if (!(player.containerMenu instanceof StorageViewerMenu menu)) return;
                        StorageInterfaceBlockEntity storageInterface = menu.getStorageInterface();
                        if (storageInterface == null) return;

                        ItemStack toExtract = payload.stack();
                        int amountNeeded = payload.amount();
                        ItemStack result = ItemStack.EMPTY;

                        for (StorageViewerMenu.DrawerSlotRef ref : payload.sources()) {
                            if (amountNeeded <= 0) break;
                            BlockEntity be = level.getBlockEntity(ref.pos());
                            if (be instanceof DrawerBlockEntity drawer) {
                                ItemStack extracted = drawer.extractItem(ref.slotIndex(), amountNeeded, false);
                                if (!extracted.isEmpty()) {
                                    if (result.isEmpty()) {
                                        result = extracted;
                                    } else {
                                        result.grow(extracted.getCount());
                                    }
                                    amountNeeded -= extracted.getCount();
                                }
                            } else if (be instanceof CompactingDrawerBlockEntity compacting) {
                                ItemStack extracted = compacting.extractItem(ref.slotIndex(), amountNeeded, false);
                                if (!extracted.isEmpty()) {
                                    if (result.isEmpty()) {
                                        result = extracted;
                                    } else {
                                        result.grow(extracted.getCount());
                                    }
                                    amountNeeded -= extracted.getCount();
                                }
                            }
                        }

                        if (!result.isEmpty()) {
                            if (!player.getInventory().add(result)) {
                                player.drop(result, false);
                            }
                        }

                        storageInterface.refreshNetworkNodes();
                        List<StorageViewerMenu.NetworkSlot> updatedSlots =
                                new ArrayList<>(storageInterface.getNetwork().getItems());
                        if (player instanceof ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(serverPlayer,
                                    new StorageViewerSyncPayload(updatedSlots));
                        }
                    });
                }
        );

        registrar.playToClient(
                StorageViewerSyncPayload.TYPE,
                StorageViewerSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.player.containerMenu instanceof StorageViewerMenu menu) {
                            menu.setNetworkSlots(payload.slots());
                            if (mc.screen instanceof StorageViewerScreen screen) {
                                screen.rebuildFilteredSlots();
                            }
                        }
                    });
                }
        );
    }
}