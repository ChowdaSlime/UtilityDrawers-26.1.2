package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateWirelessFluidDrawerPayload(BlockPos pos, WirelessNetworkKey key) implements CustomPacketPayload {

    public static final Type<UpdateWirelessFluidDrawerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "update_wireless_fluid_drawer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWirelessFluidDrawerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UpdateWirelessFluidDrawerPayload::pos,
                    WirelessNetworkKey.STREAM_CODEC.cast(), UpdateWirelessFluidDrawerPayload::key,
                    UpdateWirelessFluidDrawerPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}