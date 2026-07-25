package net.chowdaslime.utilitydrawers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.chowdaslime.utilitydrawers.UtilityDrawers;

public record CycleViewerSizePayload(int newRows) implements CustomPacketPayload {
    public static final Type<CycleViewerSizePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "cycle_viewer_size"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleViewerSizePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CycleViewerSizePayload::newRows,
            CycleViewerSizePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}