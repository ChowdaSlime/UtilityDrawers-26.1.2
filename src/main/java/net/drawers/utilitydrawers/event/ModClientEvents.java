package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.ModBlockEntities;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.client.*;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.DrawerScreen;
import net.drawers.utilitydrawers.menu.FluidDrawerScreen;
import net.drawers.utilitydrawers.menu.ModMenuTypes;
import net.drawers.utilitydrawers.network.CycleRemoteModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRAWER_BLOCK_ENTITY.get(), DrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_DRAWER_BLOCK_ENTITY.get(), FluidDrawerRenderer::new);
    }

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DrawerTooltipComponent.class, ClientDrawerTooltipComponent::new);
        event.register(FluidDrawerTooltipComponent.class, ClientFluidDrawerTooltipComponent::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DRAWER_MENU.get(), DrawerScreen::new);
        event.register(ModMenuTypes.FLUID_DRAWER_MENU.get(), FluidDrawerScreen::new);
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen != null) return;
        if (!mc.options.keyUse.isDown()) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return;

        var fluidHandlerOpt = FluidUtil.getFluidHandler(stack.copyWithCount(1));
        if (fluidHandlerOpt.isEmpty()) return;

        FluidStack simDrain = fluidHandlerOpt.get().drain(1, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        if (simDrain.isEmpty()) return;

        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof StorageInterfaceBlockEntity)) return;

        net.minecraft.network.protocol.game.ServerboundUseItemOnPacket packet =
                new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                        InteractionHand.MAIN_HAND,
                        blockHit,
                        mc.player.containerMenu.getStateId()
                );
        mc.player.connection.send(packet);
    }
}