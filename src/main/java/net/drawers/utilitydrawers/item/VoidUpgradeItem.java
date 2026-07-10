package net.drawers.utilitydrawers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class VoidUpgradeItem extends Item {

    public VoidUpgradeItem(Properties properties) {
        super(properties.component(
                DataComponents.LORE,
                new ItemLore(List.of(
                        Component.literal("Voids Excess Items")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
                ))
        ));
    }
}