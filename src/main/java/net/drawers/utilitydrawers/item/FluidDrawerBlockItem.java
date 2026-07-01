package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.client.FluidDrawerTooltipComponent;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidDrawerBlockItem extends BlockItem {

    public FluidDrawerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<FluidStack> fluids = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            for (int i = 0; i < 4; i++) {
                String slotKey = "Slot" + i;
                if (tag.contains(slotKey)) {
                    tag.getCompound(slotKey).ifPresent(slotTag -> {
                        slotTag.getCompound("Fluid").ifPresent(fluidTag -> {
                            var registries = Minecraft.getInstance().level.registryAccess();
                            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                            Optional<FluidStack> parsedOpt = FluidStack.CODEC.parse(ops, fluidTag).resultOrPartial();
                            if (parsedOpt.isPresent() && !parsedOpt.get().isEmpty()) {
                                long amount = slotTag.getLong("Amount").orElse((long) parsedOpt.get().getAmount());
                                if (amount > 0) {
                                    fluids.add(parsedOpt.get());
                                    amounts.add(amount);
                                }
                            }
                        });
                    });
                }
            }
        }

        WirelessNetworkKey networkKey = stack.get(ModDataComponents.WIRELESS_NETWORK_KEY);
        if (fluids.isEmpty() && networkKey == null) return Optional.empty();
        return Optional.of(new FluidDrawerTooltipComponent(fluids, amounts, networkKey));
    }
}