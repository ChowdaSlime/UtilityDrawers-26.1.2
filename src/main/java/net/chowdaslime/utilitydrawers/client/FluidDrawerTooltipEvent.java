package net.chowdaslime.utilitydrawers.client;

import com.mojang.datafixers.util.Either;
import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.block.FluidDrawerBlock;
import net.chowdaslime.utilitydrawers.block.FramedFluidDrawerBlock;
import net.chowdaslime.utilitydrawers.data.ModDataComponents;
import net.chowdaslime.utilitydrawers.data.WirelessNetworkKey;
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
import java.util.Optional;

import static net.chowdaslime.utilitydrawers.client.ClientFluidDrawerTooltipComponent.formatMillibuckets;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class FluidDrawerTooltipEvent {

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        if (blockItem.getBlock() instanceof FluidDrawerBlock ||
                blockItem.getBlock() instanceof FramedFluidDrawerBlock) {
            handleFluidDrawerTooltip(event, stack);
        }
    }

    private static void handleFluidDrawerTooltip(RenderTooltipEvent.GatherComponents event, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        WirelessNetworkKey networkKey = stack.get(ModDataComponents.WIRELESS_NETWORK_KEY);

        List<FluidStack> fluids = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();

        if (customData != null) {
            CompoundTag tag = customData.copyTag();

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
                                    fluids.add(storedFluid);
                                    amounts.add(amount);
                                }
                            }
                        })
                );
            }
        }

        if (!fluids.isEmpty() || networkKey != null) {
            event.getTooltipElements().add(Either.right(new FluidDrawerTooltipComponent(fluids, amounts, networkKey)));
            for (int i = 0; i < fluids.size(); i++) {
                event.getTooltipElements().add(Either.left(
                        Component.literal("§7- " + formatMillibuckets(amounts.get(i)) + " §b" + fluids.get(i).getHoverName().getString())
                ));
            }
        } else {
            event.getTooltipElements().add(Either.left(Component.literal("§7(Empty)")));
        }
    }
}