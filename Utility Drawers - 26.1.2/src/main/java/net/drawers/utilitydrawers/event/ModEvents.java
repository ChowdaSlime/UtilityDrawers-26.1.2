package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof DrawerBlock drawerBlock)) return;
        if (event.getFace() != state.getValue(DrawerBlock.FACING)) return;

        event.setCanceled(true);

        if (!event.getLevel().isClientSide()) {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof DrawerBlockEntity drawer) {
                Player player = event.getEntity();

                double reach = player.blockInteractionRange();
                net.minecraft.world.phys.HitResult hitResult = player.pick(reach, 0, false);

                int targetSlot = 0;
                if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                    targetSlot = drawerBlock.getTargetSlot(blockHit.getLocation(), state, drawer.getSlotCount());
                }

                if (!drawer.isSlotEmpty(targetSlot)) {
                    int amount = player.isShiftKeyDown()
                            ? drawer.getStoredItem(targetSlot).getMaxStackSize() : 1;
                    ItemStack extracted = drawer.extractItem(targetSlot, amount, false);

                    if (!extracted.isEmpty()) {
                        if (!player.getInventory().add(extracted)) {
                            player.drop(extracted, false);
                        }
                    }
                }
            }
        }
    }
}