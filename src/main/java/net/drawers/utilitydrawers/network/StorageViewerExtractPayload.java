package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record StorageViewerExtractPayload(
        ItemStack stack,
        int amount,
        List<StorageViewerMenu.DrawerSlotRef> sources,
        ExtractMode mode
) implements CustomPacketPayload {

    public enum ExtractMode { CURSOR_FULL, CURSOR_HALF, SHIFT_MOVE }

    public static final Type<StorageViewerExtractPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "storage_viewer_extract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerMenu.DrawerSlotRef> REF_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, StorageViewerMenu.DrawerSlotRef::pos,
                    ByteBufCodecs.INT, StorageViewerMenu.DrawerSlotRef::slotIndex,
                    StorageViewerMenu.DrawerSlotRef::new
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, ExtractMode> MODE_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(
                    i -> ExtractMode.values()[i],
                    Enum::ordinal
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerExtractPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, StorageViewerExtractPayload::stack,
                    ByteBufCodecs.INT, StorageViewerExtractPayload::amount,
                    REF_CODEC.apply(ByteBufCodecs.list()), StorageViewerExtractPayload::sources,
                    MODE_CODEC, StorageViewerExtractPayload::mode,
                    StorageViewerExtractPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}