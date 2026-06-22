package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.client.DrawerTooltipComponent;
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
        List<ItemStack> items = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            String slotKey = "Slot" + i;
            if (tag.contains(slotKey)) {
                tag.getCompound(slotKey).ifPresent(slotTag -> {
                    slotTag.getCompound("Item").ifPresent(itemTag -> {
                        var registries = Minecraft.getInstance().level.registryAccess();
                        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                        Optional<ItemStack> parsedOpt = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();
                        if (parsedOpt.isPresent() && !parsedOpt.get().isEmpty()) {
                            long count = slotTag.getLong("Count").orElse((long) parsedOpt.get().getCount());
                            if (count > 0) {
                                items.add(parsedOpt.get());
                                counts.add(count);
                            }
                        }
                    });
                });
            }
        }

        if (items.isEmpty()) return Optional.empty();
        return Optional.of(new DrawerTooltipComponent(items, counts));
    }
}