package net.chowdaslime.utilitydrawers.network;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.menu.CraftingStorageViewerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record JeiRecipeTransferPayload(Map<Integer, ItemStack> inputs, boolean maxTransfer) implements CustomPacketPayload {

    public static final Type<JeiRecipeTransferPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "jei_recipe_transfer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JeiRecipeTransferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ItemStack.OPTIONAL_STREAM_CODEC),
            JeiRecipeTransferPayload::inputs,
            ByteBufCodecs.BOOL,
            JeiRecipeTransferPayload::maxTransfer,
            JeiRecipeTransferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof CraftingStorageViewerMenu menu) {
                menu.handleJeiTransfer(this.inputs, this.maxTransfer);
            }
        });
    }
}