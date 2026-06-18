package net.drawers.utilitydrawers.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ClientDrawerTooltipComponent implements ClientTooltipComponent {

    private final DrawerTooltipComponent drawerComponent;
    private static final int SLOT_SIZE = 20;

    public ClientDrawerTooltipComponent(DrawerTooltipComponent drawerComponent) {
        this.drawerComponent = drawerComponent;
    }

    @Override
    public int getHeight(Font font) {
        return SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        int maxTextWidth = 0;
        for (int i = 0; i < drawerComponent.counts().size(); i++) {
            String countText = String.valueOf(drawerComponent.counts().get(i));
            maxTextWidth = Math.max(maxTextWidth, font.width(countText));
        }
        return Math.max(drawerComponent.items().size() * SLOT_SIZE, maxTextWidth + SLOT_SIZE);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        for (int i = 0; i < drawerComponent.items().size(); i++) {
            ItemStack stack = drawerComponent.items().get(i);
            long count = drawerComponent.counts().get(i);
            int slotX = x + (i * SLOT_SIZE);

            graphics.item(stack, slotX + 1, y + 1);

            String countText = String.valueOf(count);
            graphics.text(font, countText, slotX + SLOT_SIZE - 2 - font.width(countText), y + SLOT_SIZE - 9, 0xFFFFFF, true);
        }
    }
}