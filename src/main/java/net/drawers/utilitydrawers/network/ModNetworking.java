package net.drawers.utilitydrawers.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "utilitydrawers")
public class ModNetworking {

    @SubscribeEvent
    public static void registerNetworkHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToClient(
                SyncNetworkSlotsPayload.TYPE,
                SyncNetworkSlotsPayload.STREAM_CODEC,
                SyncNetworkSlotsPayload::handleDataSync
        );
    }
}