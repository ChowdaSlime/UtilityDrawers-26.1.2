package net.drawers.utilitydrawers.client;

import com.mojang.datafixers.util.Either;
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
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class FluidDrawerTooltipEvent {

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof FluidDrawerBlock)) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        List<FluidStack> fluids = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            String slotKey = "Slot" + i;
            if (!tag.contains(slotKey)) continue;

            tag.getCompound(slotKey).ifPresent(slotTag ->
                    slotTag.getCompound("Fluid").ifPresent(fluidTag -> {
                        var registries = Minecraft.getInstance().level.registryAccess();
                        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

                        FluidStack.CODEC.parse(ops, fluidTag).resultOrPartial().ifPresent(fluid -> {
                            if (!fluid.isEmpty()) {
                                long amount = slotTag.getLong("Amount").orElse((long) fluid.getAmount());
                                if (amount > 0) {
                                    fluids.add(fluid);
                                    amounts.add(amount);
                                }
                            }
                        });
                    })
            );
        }

        if (fluids.isEmpty()) {
            event.getTooltipElements().add(Either.left(Component.literal("§7(Empty)")));
        } else {
            // Add the visual component first
            event.getTooltipElements().add(Either.right(new FluidDrawerTooltipComponent(fluids, amounts)));
            // Then add a text line per slot underneath
            for (int i = 0; i < fluids.size(); i++) {
                String displayAmount = formatMillibuckets(amounts.get(i));
                event.getTooltipElements().add(Either.left(
                        Component.literal("§7- " + displayAmount + " §b" + fluids.get(i).getHoverName().getString())
                ));
            }
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