package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateWirelessDrawerPayload(BlockPos pos, WirelessNetworkKey key) implements CustomPacketPayload {

    public static final Type<UpdateWirelessDrawerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "update_wireless_drawer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWirelessDrawerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdateWirelessDrawerPayload::pos,
                    WirelessNetworkKey.STREAM_CODEC.cast(), UpdateWirelessDrawerPayload::key,
                    UpdateWirelessDrawerPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}