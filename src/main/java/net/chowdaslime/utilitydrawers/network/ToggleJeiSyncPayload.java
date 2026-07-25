package net.chowdaslime.utilitydrawers.network;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleJeiSyncPayload(boolean syncJei) implements CustomPacketPayload {
    public static final Type<ToggleJeiSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "toggle_jei_sync"));

    public static final StreamCodec<FriendlyByteBuf, ToggleJeiSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ToggleJeiSyncPayload::syncJei,
                    ToggleJeiSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}