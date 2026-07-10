package net.drawers.utilitydrawers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class DrawerUpgradeItem extends Item {

    private final int tier;

    public int getMultiplier() {
        return getMultiplierForTier(this.tier);
    }

    private static int getMultiplierForTier(int tier) {
        return switch (tier) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            case 4 -> 32;
            default -> 1;
        };
    }

    public DrawerUpgradeItem(Properties properties, int tier) {
        super(properties.component(
                DataComponents.LORE,
                new ItemLore(List.of(
                        Component.literal("Multiplier: " + getMultiplierForTier(tier) + "x")
                                .withStyle(ChatFormatting.BLUE)
                ))
        ));
        this.tier = tier;
    }
}