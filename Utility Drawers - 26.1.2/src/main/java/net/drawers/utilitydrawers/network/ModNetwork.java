package net.drawers.utilitydrawers.network;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModNetwork {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(UtilityDrawers.MODID);

        registrar.playToServer(
                CycleRemoteModePayload.TYPE,
                CycleRemoteModePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        ItemStack stack = player.getMainHandItem();

                        if (stack.getItem() instanceof StorageRemoteItem) {
                            StorageRemoteItem.cycleMode(stack);

                            boolean isLink = StorageRemoteItem.isLinkMode(stack);
                            ChatFormatting modeColor = isLink ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE;
                            String modeName = isLink ? "Link/Unlink" : "Lock/Unlock";

                            player.sendOverlayMessage(
                                    Component.literal("Mode: ")
                                            .append(Component.literal(modeName).withStyle(modeColor))
                            );
                        }
                    });
                }
        );
    }
}
