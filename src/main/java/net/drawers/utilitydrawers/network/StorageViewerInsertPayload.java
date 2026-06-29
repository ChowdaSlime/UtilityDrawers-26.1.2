package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record StorageViewerInsertPayload(
        ItemStack stack,
        int amount,
        boolean insertAsFluid
) implements CustomPacketPayload {

    public static final Type<StorageViewerInsertPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "storage_viewer_insert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerInsertPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, StorageViewerInsertPayload::stack,
                    ByteBufCodecs.INT, StorageViewerInsertPayload::amount,
                    ByteBufCodecs.BOOL, StorageViewerInsertPayload::insertAsFluid,
                    StorageViewerInsertPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}