package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static net.drawers.utilitydrawers.UtilityDrawers.MODID;

public record ToggleSelectModePacket() implements CustomPacketPayload {

    public static final Type<ToggleSelectModePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MODID, "toggle_select_mode"));

    public static final StreamCodec<FriendlyByteBuf, ToggleSelectModePacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleSelectModePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSelectModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof StorageRemoteItem) {
                StorageRemoteItem.onShiftLeftClickAir(stack, player);
            }
        });
    }
}