package net.drawers.utilitydrawers.client;

import com.mojang.datafixers.util.Either;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.FramedDrawerBlock;
import net.drawers.utilitydrawers.data.ModDataComponents;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class DrawerTooltipEvent {

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        if (blockItem.getBlock() instanceof DrawerBlock ||
                blockItem.getBlock() instanceof FramedDrawerBlock) {
            handleItemDrawerTooltip(event, stack);
        }
    }

    private static void handleItemDrawerTooltip(RenderTooltipEvent.GatherComponents event, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        WirelessNetworkKey networkKey = stack.get(ModDataComponents.WIRELESS_NETWORK_KEY);

        List<ItemStack> items = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            for (int i = 0; i < 4; i++) {
                String slotKey = "Slot" + i;
                if (!tag.contains(slotKey)) continue;

                tag.getCompound(slotKey).ifPresent(slotTag ->
                        slotTag.getCompound("Item").ifPresent(itemTag -> {
                            var registries = Minecraft.getInstance().level.registryAccess();
                            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                            Optional<ItemStack> parsedStackOpt = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();

                            if (parsedStackOpt.isPresent() && !parsedStackOpt.get().isEmpty()) {
                                ItemStack storedStack = parsedStackOpt.get();
                                long count = slotTag.getLong("Count").orElse((long) storedStack.getCount());

                                if (count > 0) {
                                    items.add(storedStack);
                                    counts.add(count);
                                }
                            }
                        })
                );
            }
        }

        if (!items.isEmpty() || networkKey != null) {
            event.getTooltipElements().add(Either.right(new DrawerTooltipComponent(items, counts, networkKey)));

            for (int i = 0; i < items.size(); i++) {
                event.getTooltipElements().add(Either.left(
                        Component.literal("§7- " + counts.get(i) + "x §b" + items.get(i).getHoverName().getString())
                ));
            }
        } else {
            event.getTooltipElements().add(Either.left(Component.literal("§7(Empty)")));
        }
    }
}