package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncPreferencesPayload(boolean sortByCount, boolean sortAscending) implements CustomPacketPayload {

    public static final Type<SyncPreferencesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "sync_preferences"));

    public static final StreamCodec<FriendlyByteBuf, SyncPreferencesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SyncPreferencesPayload::sortByCount,
                    ByteBufCodecs.BOOL, SyncPreferencesPayload::sortAscending,
                    SyncPreferencesPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}