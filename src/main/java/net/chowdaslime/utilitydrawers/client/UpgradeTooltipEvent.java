package net.chowdaslime.utilitydrawers.client;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.item.ExtractUpgradeItem;
import net.chowdaslime.utilitydrawers.item.InsertUpgradeItem;
import net.chowdaslime.utilitydrawers.menu.CompactingDrawerMenu;
import net.chowdaslime.utilitydrawers.menu.DrawerMenu;
import net.chowdaslime.utilitydrawers.menu.FluidDrawerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class UpgradeTooltipEvent {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof ExtractUpgradeItem ||
                event.getItemStack().getItem() instanceof InsertUpgradeItem) {
            event.getToolTip().removeIf(c -> c.getString().matches(".* x\\d+$"));

            Minecraft mc = Minecraft.getInstance();
            Component instruction;

            if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen &&
                    (screen.getMenu() instanceof DrawerMenu ||
                            screen.getMenu() instanceof FluidDrawerMenu ||
                            screen.getMenu() instanceof CompactingDrawerMenu)) {

                instruction = Component.literal("Press ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.keybind("key.utilitydrawers.open_upgrade_config").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" in drawer to configure").withStyle(ChatFormatting.GRAY));
            } else {
                instruction = Component.literal("Right-click ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("in hand to configure").withStyle(ChatFormatting.GRAY));
            }

            event.getToolTip().add(1, instruction);
        }
    }
}