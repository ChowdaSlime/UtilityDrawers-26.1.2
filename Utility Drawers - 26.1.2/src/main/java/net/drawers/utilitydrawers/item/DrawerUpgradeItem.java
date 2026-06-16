package net.drawers.utilitydrawers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class DrawerUpgradeItem extends Item {

    private final int tier;

    public DrawerUpgradeItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getMultiplier() {
        return switch (tier) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            case 4 -> 32;
            default -> 1;
        };
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag flag) {

        builder.accept(
                Component.literal("Storage Multiplier: " + getMultiplier() + "x")
                        .withStyle(ChatFormatting.BLUE)
        );

        super.appendHoverText(stack, context, display, builder, flag);
    }

}
