package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.ModBlockEntities;
import net.drawers.utilitydrawers.client.ClientDrawerTooltipComponent;
import net.drawers.utilitydrawers.client.DrawerRenderer;
import net.drawers.utilitydrawers.client.DrawerTooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

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
}