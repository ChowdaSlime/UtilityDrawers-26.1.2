package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record CompactingDrawerTooltipComponent(List<ItemStack> items, List<Long> counts, WirelessNetworkKey networkKey) implements TooltipComponent {
}