package net.drawers.utilitydrawers.event;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UtilityDrawers.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());

        if (state.getBlock() instanceof DrawerBlock) {
            if (event.getFace() == state.getValue(DrawerBlock.FACING)) {
                event.setCanceled(true);

                if (!event.getLevel().isClientSide()) {
                    if (event.getLevel().getBlockEntity(event.getPos()) instanceof DrawerBlockEntity drawer) {
                        for (int i = 0; i < drawer.getSlotCount(); i++) {
                            if (!drawer.isSlotEmpty(i)) {
                                int amount = event.getEntity().isShiftKeyDown() ? drawer.getStoredItem(i).getMaxStackSize() : 1;
                                ItemStack extracted = drawer.extractItem(i, amount, false);

                                if (!extracted.isEmpty()) {
                                    event.getEntity().getInventory().add(extracted);
                                    if (extracted.getCount() > 0) {
                                        event.getEntity().drop(extracted, false);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}