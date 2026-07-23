package net.chowdaslime.utilitydrawers.network;

import net.chowdaslime.utilitydrawers.menu.UpgradeConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenUpgradeConfigPayloadHandler {

    public static void handle(OpenUpgradeConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            BlockPos pos = payload.pos();
            int slotIndex = payload.slotIndex();

            serverPlayer.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new UpgradeConfigMenu(containerId, playerInventory, pos, slotIndex),
                            Component.literal("Configure Upgrade")),
                    buf -> {
                        buf.writeBoolean(true);
                        buf.writeBlockPos(pos);
                        buf.writeVarInt(slotIndex);
                    });
        });
    }
}