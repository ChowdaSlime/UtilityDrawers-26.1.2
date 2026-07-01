package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.WirelessDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.WirelessFluidDrawerBlockEntity;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.drawers.utilitydrawers.menu.StorageViewerScreen;
import net.drawers.utilitydrawers.menu.WirelessDrawerMenu;
import net.drawers.utilitydrawers.menu.WirelessFluidDrawerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void registerNetworkHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToClient(
                SyncNetworkSlotsPayload.TYPE,
                SyncNetworkSlotsPayload.STREAM_CODEC,
                SyncNetworkSlotsPayload::handleDataSync
        );

        registrar.playToServer(
                StorageViewerExtractPayload.TYPE,
                StorageViewerExtractPayload.STREAM_CODEC,
                StorageViewerExtractHandler::handle
        );

        registrar.playToServer(
                StorageViewerInsertPayload.TYPE,
                StorageViewerInsertPayload.STREAM_CODEC,
                StorageViewerInsertHandler::handle
        );

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
                ToggleSortPayload.TYPE,
                ToggleSortPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        if (player.containerMenu instanceof StorageViewerMenu menu) {
                            menu.sortAscending = payload.sortAscending();
                            menu.saveSortPreference(payload.sortByCount());
                        }
                    });
                }
        );

        registrar.playToServer(
                ToggleSelectModePacket.TYPE,
                ToggleSelectModePacket.STREAM_CODEC,
                ToggleSelectModePacket::handle
        );

        registrar.playToServer(
                UpdateWirelessDrawerPayload.TYPE,
                UpdateWirelessDrawerPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        if (player.level().getBlockEntity(payload.pos()) instanceof WirelessDrawerBlockEntity wirelessBE) {
                            WirelessNetworkKey received = payload.key();
                            WirelessNetworkKey trusted = received.isPublic()
                                    ? received
                                    : new WirelessNetworkKey(
                                    received.color1(), received.color2(), received.color3(),
                                    false,
                                    Optional.of(player.getUUID()),
                                    received.slotCount()
                            );
                            wirelessBE.setNetworkKey(trusted);
                            if (player.containerMenu instanceof WirelessDrawerMenu wMenu) {
                                wMenu.refreshFromBlockEntity();
                            }
                        }
                    });
                }
        );

        registrar.playToServer(
                UpdateWirelessFluidDrawerPayload.TYPE,
                UpdateWirelessFluidDrawerPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        if (player.level().getBlockEntity(payload.pos()) instanceof WirelessFluidDrawerBlockEntity wirelessBE) {
                            WirelessNetworkKey received = payload.key();
                            WirelessNetworkKey trusted = received.isPublic()
                                    ? received
                                    : new WirelessNetworkKey(
                                    received.color1(), received.color2(), received.color3(),
                                    false,
                                    Optional.of(player.getUUID()),
                                    received.slotCount()
                            );
                            wirelessBE.setNetworkKey(trusted);
                            if (player.containerMenu instanceof WirelessFluidDrawerMenu wMenu) {
                                wMenu.refreshFromBlockEntity();
                            }
                        }
                    });
                }
        );

        registrar.playToClient(
                SyncPreferencesPayload.TYPE,
                SyncPreferencesPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.screen instanceof StorageViewerScreen screen) {
                            StorageViewerMenu menu = screen.getMenu();
                            menu.sortByCount = payload.sortByCount();
                            menu.sortAscending = payload.sortAscending();
                            screen.rebuildFilteredSlots();
                        }
                    });
                }
        );
    }
}