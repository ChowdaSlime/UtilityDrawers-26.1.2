package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CycleRemoteModePayload() implements CustomPacketPayload {

    // Using fromNamespaceAndPath based on your specific mapping
    public static final Type<CycleRemoteModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "cycle_remote_mode"));

    public static final StreamCodec<FriendlyByteBuf, CycleRemoteModePayload> STREAM_CODEC = StreamCodec.unit(new CycleRemoteModePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}