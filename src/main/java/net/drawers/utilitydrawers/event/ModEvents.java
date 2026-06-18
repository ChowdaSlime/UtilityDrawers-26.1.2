package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
        if (!(state.getBlock() instanceof DrawerBlock drawerBlock)) return;
        if (event.getFace() != state.getValue(DrawerBlock.FACING)) return;

        BlockPos pos = event.getPos();

        double reach = player.blockInteractionRange();
        net.minecraft.world.phys.HitResult hitResult = player.pick(reach, 0, false);
        int targetSlot = -1;
        if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            if (event.getLevel().getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                targetSlot = drawerBlock.getTargetSlot(blockHit.getLocation(), state, drawer.getSlotCount());
            }
        }

        if (targetSlot < 0) return;

        event.setCanceled(true);

        if (!event.getLevel().isClientSide()) {
            long currentTick = event.getLevel().getGameTime();
            long lastTick = lastClickTick.getOrDefault(uuid, 0L);

            if (!pos.equals(heldDrawer.get(uuid)) || (currentTick - lastTick > 2)) {
                heldDrawer.put(uuid, pos);
                lastClickTick.put(uuid, currentTick);

                if (event.getLevel().getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
                    if (!drawer.isSlotEmpty(targetSlot)) {
                        int amount = player.isShiftKeyDown()
                                ? drawer.getStoredItem(targetSlot).getMaxStackSize() : 1;
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
    }
}