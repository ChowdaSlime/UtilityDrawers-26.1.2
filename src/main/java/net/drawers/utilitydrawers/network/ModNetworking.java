package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.drawers.utilitydrawers.menu.StorageViewerScreen;
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
                            menu.sortAscending = payload.sortAscending(); // add this
                            menu.saveSortPreference(payload.sortByCount());
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