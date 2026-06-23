package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.client.CompactingDrawerTooltipComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompactingDrawerBlockItem extends BlockItem {

    public CompactingDrawerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();

        CompoundTag tag = customData.copyTag();
        var registries = Minecraft.getInstance().level.registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        long rawCount = tag.getLongOr("RawCount", 0L);
        int ratio0 = tag.getIntOr("Ratio0", 9);
        int ratio1 = tag.getIntOr("Ratio1", 9);

        String[] keys  = {"BlockItem", "MidItem", "BaseItem"};
        long[] counts = {rawCount / ((long) ratio0 * ratio1), rawCount / ratio0, rawCount};

        List<ItemStack> items      = new ArrayList<>();
        List<Long>      itemCounts = new ArrayList<>();

        for (int i = 0; i < keys.length; i++) {
            if (!tag.contains(keys[i])) continue;
            var itemTag = tag.get(keys[i]);
            if (itemTag == null) continue;

            Optional<ItemStack> parsed = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();
            if (parsed.isEmpty() || parsed.get().isEmpty()) continue;
            if (counts[i] <= 0) continue;

            items.add(parsed.get());
            itemCounts.add(counts[i]);
        }

        if (items.isEmpty()) return Optional.empty();
        return Optional.of(new CompactingDrawerTooltipComponent(items, itemCounts));
    }
}