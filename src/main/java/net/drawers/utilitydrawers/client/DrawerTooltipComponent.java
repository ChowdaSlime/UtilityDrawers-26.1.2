package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record DrawerTooltipComponent(List<ItemStack> items, List<Long> counts, WirelessNetworkKey networkKey) implements TooltipComponent {
}