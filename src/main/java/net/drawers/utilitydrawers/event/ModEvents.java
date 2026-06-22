package net.drawers.utilitydrawers.event;

import appeng.api.AECapabilities;
import net.drawers.utilitydrawers.ae2.CompactingDrawerMEStorage;
import net.drawers.utilitydrawers.ae2.DrawerMEStorage;
import net.drawers.utilitydrawers.ae2.FluidDrawerMEStorage;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.ae2.StorageInterfaceMEStorage;
import net.drawers.utilitydrawers.block.CompactingDrawerBlock;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModEvents {

    private static final Map<UUID, BlockPos> heldDrawer = new HashMap<>();
    private static final Map<UUID, Long> lastClickTick = new HashMap<>();

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            heldDrawer.remove(uuid);
            return;
        }

        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        BlockPos pos = event.getPos();
        double reach = player.blockInteractionRange();
        net.minecraft.world.phys.HitResult hitResult = player.pick(reach, 0, false);
        int targetSlot = -1;

        if (state.getBlock() instanceof DrawerBlock drawerBlock) {
            if (event.getFace() != state.getValue(DrawerBlock.FACING)) return;

            if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                if (event.getLevel().getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                    targetSlot = drawerBlock.getTargetSlot(blockHit.getLocation(), state, drawer.getSlotCount());

                    if (targetSlot >= 0) {
                        event.setCanceled(true);
                        handleExtraction(event, player, uuid, pos, targetSlot, drawer);
                    }
                }
            }
        }
        else if (state.getBlock() instanceof CompactingDrawerBlock compactingBlock) {
            if (event.getFace() != state.getValue(CompactingDrawerBlock.FACING)) return;

            if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                if (event.getLevel().getBlockEntity(pos) instanceof CompactingDrawerBlockEntity drawer) {
                    targetSlot = compactingBlock.getTargetSlot(blockHit.getLocation(), state);

                    if (targetSlot >= 0) {
                        event.setCanceled(true);
                        handleCompactingExtraction(event, player, uuid, pos, targetSlot, drawer);
                    }
                }
            }
        }
    }

    private static void handleExtraction(PlayerInteractEvent.LeftClickBlock event, Player player, UUID uuid, BlockPos pos, int targetSlot, DrawerBlockEntity drawer) {
        if (!event.getLevel().isClientSide()) {
            long currentTick = event.getLevel().getGameTime();
            long lastTick = lastClickTick.getOrDefault(uuid, 0L);

            if (!pos.equals(heldDrawer.get(uuid)) || (currentTick - lastTick > 2)) {
                heldDrawer.put(uuid, pos);
                lastClickTick.put(uuid, currentTick);

                if (!drawer.isSlotEmpty(targetSlot)) {
                    int amount = player.isShiftKeyDown() ? drawer.getStoredItem(targetSlot).getMaxStackSize() : 1;
                    ItemStack extracted = drawer.extractItem(targetSlot, amount, false);

                    if (!extracted.isEmpty()) {
                        if (!player.getInventory().add(extracted)) {
                            player.drop(extracted, false);
                        }
                        player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    private static void handleCompactingExtraction(PlayerInteractEvent.LeftClickBlock event, Player player, UUID uuid, BlockPos pos, int targetSlot, CompactingDrawerBlockEntity drawer) {
        if (!event.getLevel().isClientSide()) {
            long currentTick = event.getLevel().getGameTime();
            long lastTick = lastClickTick.getOrDefault(uuid, 0L);

            if (!pos.equals(heldDrawer.get(uuid)) || (currentTick - lastTick > 2)) {
                heldDrawer.put(uuid, pos);
                lastClickTick.put(uuid, currentTick);

                if (!drawer.isSlotEmpty(targetSlot)) {
                    int amount = player.isShiftKeyDown() ? drawer.getStoredItem(targetSlot).getMaxStackSize() : 1;
                    ItemStack extracted = drawer.extractItem(targetSlot, amount, false);

                    if (!extracted.isEmpty()) {
                        if (!player.getInventory().add(extracted)) {
                            player.drop(extracted, false);
                        }
                        player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        heldDrawer.remove(event.getEntity().getUUID());
        lastClickTick.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof DrawerBlockEntity drawer) {
            drawer.unlinkFromInterfaces();
        }

        if (event.getLevel().getBlockEntity(event.getPos()) instanceof FluidDrawerBlockEntity drawer) {
            drawer.unlinkFromInterfaces();
        }

        // Ensure compacting drawer is properly unlinked from the network when broken!
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof CompactingDrawerBlockEntity drawer) {
            drawer.unlinkFromInterfaces();
        }
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                ModBlockEntities.DRAWER_BLOCK_ENTITY.get(),
                (drawer, side) -> new DrawerMEStorage(drawer)
        );

        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                ModBlockEntities.FLUID_DRAWER_BLOCK_ENTITY.get(),
                (drawer, side) -> new FluidDrawerMEStorage(drawer)
        );

        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                ModBlockEntities.STORAGE_INTERFACE_BLOCK_ENTITY.get(),
                (interfaceEntity, side) -> new StorageInterfaceMEStorage(interfaceEntity)
        );

        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                ModBlockEntities.COMPACTING_DRAWER_BLOCK_ENTITY.get(),
                (interfaceEntity, side) -> new CompactingDrawerMEStorage(interfaceEntity)
        );

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.DRAWER_BLOCK_ENTITY.get(),
                (drawer, side) -> drawer.createItemHandler()
        );

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.FLUID_DRAWER_BLOCK_ENTITY.get(),
                (drawer, side) -> drawer.createFluidHandler()
        );

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.STORAGE_INTERFACE_BLOCK_ENTITY.get(),
                (interfaceEntity, side) -> interfaceEntity.createItemHandler()
        );

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.STORAGE_INTERFACE_BLOCK_ENTITY.get(),
                (interfaceEntity, side) -> interfaceEntity.createFluidHandler()
        );

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COMPACTING_DRAWER_BLOCK_ENTITY.get(),
                (drawer, side) -> drawer.createItemHandler()
        );
    }
}