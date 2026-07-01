package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.block.entity.ModBlockEntities;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.client.*;
import net.drawers.utilitydrawers.client.model.FramedDrawerBlockStateModel;
import net.drawers.utilitydrawers.client.model.FramedDrawerItemModel;
import net.drawers.utilitydrawers.client.model.FramedFluidDrawerBlockStateModel;
import net.drawers.utilitydrawers.client.model.FramedFluidDrawerItemModel;
import net.drawers.utilitydrawers.client.model.FramedCompactingDrawerBlockStateModel;
import net.drawers.utilitydrawers.client.model.FramedCompactingDrawerItemModel;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.menu.*;
import net.drawers.utilitydrawers.network.CycleRemoteModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRAWER_BLOCK_ENTITY.get(), DrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRAMED_DRAWER_BLOCK_ENTITY.get(), DrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_DRAWER_BLOCK_ENTITY.get(), FluidDrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRAMED_FLUID_DRAWER_BLOCK_ENTITY.get(), FluidDrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.COMPACTING_DRAWER_BLOCK_ENTITY.get(), CompactingDrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRAMED_COMPACTING_DRAWER_BLOCK_ENTITY.get(), CompactingDrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WIRELESS_DRAWER_BLOCK_ENTITY.get(), DrawerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WIRELESS_FLUID_DRAWER_BLOCK_ENTITY.get(), FluidDrawerRenderer::new);
    }

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DrawerTooltipComponent.class, ClientDrawerTooltipComponent::new);
        event.register(FluidDrawerTooltipComponent.class, ClientFluidDrawerTooltipComponent::new);
        event.register(CompactingDrawerTooltipComponent.class, ClientCompactingDrawerTooltipComponent::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DRAWER_MENU.get(), DrawerScreen<DrawerMenu>::new);
        event.register(ModMenuTypes.FLUID_DRAWER_MENU.get(), FluidDrawerScreen<FluidDrawerMenu>::new);
        event.register(ModMenuTypes.COMPACTING_DRAWER_MENU.get(), CompactingDrawerScreen::new);
        event.register(ModMenuTypes.STORAGE_INTERFACE_MENU.get(), StorageInterfaceScreen::new);
        event.register(ModMenuTypes.DRAWER_FRAMER_MENU.get(), DrawerFramerScreen::new);
        event.register(ModMenuTypes.STORAGE_VIEWER_MENU.get(), StorageViewerScreen::new);
        event.register(ModMenuTypes.WIRELESS_DRAWER_MENU.get(), WirelessDrawerScreen::new);
        event.register(ModMenuTypes.WIRELESS_FLUID_DRAWER_MENU.get(), WirelessFluidDrawerScreen::new);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof StorageRemoteItem)) return;
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

        ItemAccess access = ItemAccess.forStack(stack).oneByOne();
        ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler == null) return;

        boolean hasFluid = false;
        for (int i = 0; i < fluidHandler.size(); i++) {
            if (!fluidHandler.getResource(i).isEmpty() && fluidHandler.getAmountAsInt(i) > 0) {
                hasFluid = true;
                break;
            }
        }
        if (!hasFluid) return;

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

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Block drawerBlock : ModBlocks.getAllFramedDrawerBlocks()) {
            for (BlockState state : drawerBlock.getStateDefinition().getPossibleStates()) {
                event.getBakingResult().blockStateModels().computeIfPresent(state,
                        (s, original) -> new FramedDrawerBlockStateModel(original));
            }
            Item blockItem = drawerBlock.asItem();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(blockItem);
            event.getBakingResult().itemStackModels().computeIfPresent(itemId,
                    (key, original) -> new FramedDrawerItemModel(original));
        }

        for (Block fluidDrawerBlock : ModBlocks.getAllFramedFluidDrawerBlocks()) {
            for (BlockState state : fluidDrawerBlock.getStateDefinition().getPossibleStates()) {
                event.getBakingResult().blockStateModels().computeIfPresent(state,
                        (s, original) -> new FramedFluidDrawerBlockStateModel(original));
            }
            Item fluidBlockItem = fluidDrawerBlock.asItem();
            Identifier fluidItemId = BuiltInRegistries.ITEM.getKey(fluidBlockItem);
            event.getBakingResult().itemStackModels().computeIfPresent(fluidItemId,
                    (key, original) -> new FramedFluidDrawerItemModel(original));
        }

        Block compactingBlock = ModBlocks.FRAMED_COMPACTING_DRAWER.get();
        for (BlockState state : compactingBlock.getStateDefinition().getPossibleStates()) {
            event.getBakingResult().blockStateModels().computeIfPresent(state,
                    (s, original) -> new FramedCompactingDrawerBlockStateModel(original));
        }
        Item compactingItem = compactingBlock.asItem();
        Identifier compactingItemId = BuiltInRegistries.ITEM.getKey(compactingItem);
        event.getBakingResult().itemStackModels().computeIfPresent(compactingItemId,
                (key, original) -> new FramedCompactingDrawerItemModel(original));
    }
}