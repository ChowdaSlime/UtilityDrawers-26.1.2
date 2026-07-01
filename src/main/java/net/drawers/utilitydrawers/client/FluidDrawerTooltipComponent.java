package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record FluidDrawerTooltipComponent(List<FluidStack> fluids, List<Long> amounts, WirelessNetworkKey networkKey) implements TooltipComponent {
}