package net.chowdaslime.utilitydrawers.client;

import net.chowdaslime.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record FluidDrawerTooltipComponent(List<FluidStack> fluids, List<Long> amounts, WirelessNetworkKey networkKey) implements TooltipComponent {
}