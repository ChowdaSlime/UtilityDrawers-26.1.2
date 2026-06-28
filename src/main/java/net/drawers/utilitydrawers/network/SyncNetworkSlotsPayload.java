package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncNetworkSlotsPayload(List<StorageViewerMenu.NetworkSlot> slots) implements CustomPacketPayload {

    public static final Type<SyncNetworkSlotsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("utilitydrawers", "sync_network_slots"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNetworkSlotsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    StorageViewerMenu.NetworkSlot.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncNetworkSlotsPayload::slots,
                    SyncNetworkSlotsPayload::new);

    public static void handleDataSync(final SyncNetworkSlotsPayload payload, final IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof StorageViewerMenu menu) {
                menu.setNetworkSlots(payload.slots());
            }
        });
    }

}