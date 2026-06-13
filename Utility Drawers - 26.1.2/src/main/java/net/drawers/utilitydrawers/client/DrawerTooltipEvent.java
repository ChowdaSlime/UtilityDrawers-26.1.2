package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
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
import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class DrawerTooltipEvent {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof DrawerBlock)) {
            return;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            AtomicBoolean hasItems = new AtomicBoolean(false);

            for (int i = 0; i < 4; i++) {
                String slotKey = "Slot" + i;

                if (tag.contains(slotKey)) {
                    tag.getCompound(slotKey).ifPresent(slotTag -> {
                        slotTag.getCompound("Item").ifPresent(itemTag -> {
                            var registries = Minecraft.getInstance().level.registryAccess();
                            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                            Optional<ItemStack> parsedStackOpt = ItemStack.CODEC.parse(ops, itemTag).resultOrPartial();

                            if (parsedStackOpt.isPresent() && !parsedStackOpt.get().isEmpty()) {
                                ItemStack storedStack = parsedStackOpt.get();
                                long count = slotTag.getLong("Count").orElse((long) storedStack.getCount());

                                if (count > 0) {
                                    hasItems.set(true);
                                    event.getToolTip().add(Component.literal("§7- " + count + "x §b" + storedStack.getHoverName().getString()));
                                }
                            }
                        });
                    });
                }
            }

            if (!hasItems.get()) {
                event.getToolTip().add(Component.literal("§7(Empty)"));
            }
        }
    }
}
