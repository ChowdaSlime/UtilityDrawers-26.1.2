package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.FluidDrawerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class FluidDrawerTooltipEvent {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        if (blockItem.getBlock() instanceof FluidDrawerBlock) {
            handleFluidDrawerTooltip(event, stack);
        }
    }

    private static void handleFluidDrawerTooltip(ItemTooltipEvent event, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        AtomicBoolean hasFluids = new AtomicBoolean(false);

        for (int i = 0; i < 4; i++) {
            String slotKey = "Slot" + i;
            if (!tag.contains(slotKey)) continue;

            tag.getCompound(slotKey).ifPresent(slotTag ->
                    slotTag.getCompound("Fluid").ifPresent(fluidTag -> {
                        var registries = Minecraft.getInstance().level.registryAccess();
                        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                        Optional<FluidStack> parsedOpt = FluidStack.CODEC.parse(ops, fluidTag).resultOrPartial();

                        if (parsedOpt.isPresent() && !parsedOpt.get().isEmpty()) {
                            FluidStack storedFluid = parsedOpt.get();
                            long amount = slotTag.getLong("Amount").orElse((long) storedFluid.getAmount());

                            if (amount > 0) {
                                hasFluids.set(true);
                                event.getToolTip().add(Component.literal(
                                        "§7- " + formatMillibuckets(amount) + " §b" + storedFluid.getHoverName().getString()
                                ));
                            }
                        }
                    })
            );
        }

        if (!hasFluids.get()) {
            event.getToolTip().add(Component.literal("§7(Empty)"));
        }
    }

    private static String formatMillibuckets(long mb) {
        if (mb >= 1000) {
            long whole = mb / 1000;
            long remainder = (mb % 1000) / 100;
            return remainder == 0 ? whole + " B" : whole + "." + remainder + " B";
        }
        return mb + " mB";
    }
}