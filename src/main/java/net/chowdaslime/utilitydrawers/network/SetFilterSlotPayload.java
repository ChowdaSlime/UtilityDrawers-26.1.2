package net.chowdaslime.utilitydrawers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record SetFilterSlotPayload(int containerId, int slotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<SetFilterSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("utilitydrawers", "set_filter_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFilterSlotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetFilterSlotPayload::containerId,
                    ByteBufCodecs.VAR_INT, SetFilterSlotPayload::slotIndex,
                    ItemStack.STREAM_CODEC, SetFilterSlotPayload::stack,
                    SetFilterSlotPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}