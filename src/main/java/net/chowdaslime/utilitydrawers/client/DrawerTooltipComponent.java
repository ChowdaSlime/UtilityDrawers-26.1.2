package net.chowdaslime.utilitydrawers.client;

import net.chowdaslime.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record DrawerTooltipComponent(List<ItemStack> items, List<Long> counts, WirelessNetworkKey networkKey) implements TooltipComponent {
}