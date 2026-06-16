package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.ModBlockEntities;
import net.drawers.utilitydrawers.client.ClientDrawerTooltipComponent;
import net.drawers.utilitydrawers.client.DrawerRenderer;
import net.drawers.utilitydrawers.client.DrawerTooltipComponent;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.DrawerScreen;
import net.drawers.utilitydrawers.menu.ModMenuTypes;
import net.drawers.utilitydrawers.network.CycleRemoteModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRAWER_BLOCK_ENTITY.get(), DrawerRenderer::new);
    }

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DrawerTooltipComponent.class, ClientDrawerTooltipComponent::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DRAWER_MENU.get(), DrawerScreen::new);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || !mc.player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = mc.player.getMainHandItem();

        if (!(stack.getItem() instanceof StorageRemoteItem)) {
            return;
        }

        ClientPacketDistributor.sendToServer(new CycleRemoteModePayload());

        event.setCanceled(true);
    }

}