package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenUpgradeConfigPayload(BlockPos pos, int slotIndex) implements CustomPacketPayload {

    public static final Type<OpenUpgradeConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "open_upgrade_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenUpgradeConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenUpgradeConfigPayload::pos,
                    ByteBufCodecs.VAR_INT, OpenUpgradeConfigPayload::slotIndex,
                    OpenUpgradeConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}