package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.CompactingDrawerBlock;
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

import java.util.Optional;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class CompactingDrawerTooltipEvent {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof CompactingDrawerBlock)) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        var registries = Minecraft.getInstance().level.registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        long rawCount = tag.getLongOr("RawCount", 0L);
        int ratio0 = tag.getIntOr("Ratio0", 9);
        int ratio1 = tag.getIntOr("Ratio1", 9);

        String[] keys  = {"BlockItem", "MidItem", "BaseItem"};
        long[] counts = {rawCount / ((long) ratio0 * ratio1), rawCount / ratio0, rawCount};

        boolean hasItems = false;

        for (int i = 0; i < keys.length; i++) {
            if (!tag.contains(keys[i])) continue;
            var itemTag = tag.get(keys[i]);
            if (itemTag == null) continue;

            Optional<ItemStack> parsed = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();
            if (parsed.isEmpty() || parsed.get().isEmpty()) continue;
            if (counts[i] <= 0) continue;

            hasItems = true;
            long count = counts[i];
            event.getToolTip().add(Component.literal(
                    "§7- " + count + "x §b" + parsed.get().getHoverName().getString()
            ));
        }

        if (!hasItems) {
            event.getToolTip().add(Component.literal("§7(Empty)"));
        }
    }
}