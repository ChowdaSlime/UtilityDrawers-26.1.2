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

public record StorageViewerSyncPayload(List<StorageViewerMenu.NetworkSlot> slots) implements CustomPacketPayload {
    public static final Type<StorageViewerSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "storage_viewer_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerMenu.DrawerSlotRef> REF_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageViewerMenu.DrawerSlotRef::pos,
            ByteBufCodecs.INT, StorageViewerMenu.DrawerSlotRef::slotIndex,
            StorageViewerMenu.DrawerSlotRef::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerMenu.NetworkSlot> SLOT_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, StorageViewerMenu.NetworkSlot::stack,
            ByteBufCodecs.VAR_LONG, StorageViewerMenu.NetworkSlot::count,
            REF_CODEC.apply(ByteBufCodecs.list()), StorageViewerMenu.NetworkSlot::sources,
            StorageViewerMenu.NetworkSlot::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageViewerSyncPayload> STREAM_CODEC = StreamCodec.composite(
            SLOT_CODEC.apply(ByteBufCodecs.list()), StorageViewerSyncPayload::slots,
            StorageViewerSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}