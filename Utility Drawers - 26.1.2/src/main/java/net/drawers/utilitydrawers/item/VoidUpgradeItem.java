package net.drawers.utilitydrawers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class VoidUpgradeItem extends Item {

    public VoidUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag flag) {

        builder.accept(
                Component.literal("Voids Excess Items")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
        );

        super.appendHoverText(stack, context, display, builder, flag);
    }
}