package net.drawers.utilitydrawers.client;

import com.mojang.datafixers.util.Either;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.CompactingDrawerBlock;
import net.drawers.utilitydrawers.block.FramedCompactingDrawerBlock;
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
public class CompactingDrawerTooltipEvent {

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        if (!(blockItem.getBlock() instanceof CompactingDrawerBlock ||
                blockItem.getBlock() instanceof FramedCompactingDrawerBlock)) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        WirelessNetworkKey networkKey = stack.get(ModDataComponents.WIRELESS_NETWORK_KEY);

        List<ItemStack> items = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            var registries = Minecraft.getInstance().level.registryAccess();
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);

            long rawCount = tag.getLongOr("RawCount", 0L);
            int ratio0 = tag.getIntOr("Ratio0", 9);
            int ratio1 = tag.getIntOr("Ratio1", 9);

            String[] keys  = {"BlockItem", "MidItem", "BaseItem"};
            long[] slotCounts = {rawCount / ((long) ratio0 * ratio1), rawCount / ratio0, rawCount};

            for (int i = 0; i < keys.length; i++) {
                if (!tag.contains(keys[i])) continue;
                var itemTag = tag.get(keys[i]);
                if (itemTag == null) continue;

                Optional<ItemStack> parsed = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();
                if (parsed.isEmpty() || parsed.get().isEmpty()) continue;
                if (slotCounts[i] <= 0) continue;

                items.add(parsed.get());
                counts.add(slotCounts[i]);
            }
        }

        if (!items.isEmpty() || networkKey != null) {
            event.getTooltipElements().add(Either.right(new CompactingDrawerTooltipComponent(items, counts, networkKey)));

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