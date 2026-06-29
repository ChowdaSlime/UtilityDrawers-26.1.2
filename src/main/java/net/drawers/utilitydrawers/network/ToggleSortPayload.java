package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleSortPayload(boolean sortByCount, boolean sortAscending) implements CustomPacketPayload {

    public static final Type<ToggleSortPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "toggle_sort"));

    public static final StreamCodec<FriendlyByteBuf, ToggleSortPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ToggleSortPayload::sortByCount,
                    ByteBufCodecs.BOOL, ToggleSortPayload::sortAscending,
                    ToggleSortPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
