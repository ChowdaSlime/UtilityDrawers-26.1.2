package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ClientDrawerTooltipComponent implements ClientTooltipComponent {

    private final DrawerTooltipComponent drawerComponent;
    private static final int SLOT_SIZE = 20;
    private static final int NETWORK_INFO_HEIGHT = 15;
    private static final int COLOR_ROW_HEIGHT = 10;
    private static final int SWATCH_SIZE = 6;
    private static final int SWATCH_GAP = 2;

    public ClientDrawerTooltipComponent(DrawerTooltipComponent drawerComponent) {
        this.drawerComponent = drawerComponent;
    }

    @Override
    public int getHeight(Font font) {
        int height = 0;

        if (drawerComponent.networkKey() != null) {
            height += font.lineHeight + 2 + SWATCH_SIZE + 2;
        }

        if (!drawerComponent.items().isEmpty()) {
            height += SLOT_SIZE;
        }

        return height;
    }

    @Override
    public int getWidth(Font font) {
        int itemsWidth = 0;
        if (!drawerComponent.items().isEmpty()) {
            int maxTextWidth = 0;
            for (int i = 0; i < drawerComponent.counts().size(); i++) {
                String countText = String.valueOf(drawerComponent.counts().get(i));
                maxTextWidth = Math.max(maxTextWidth, font.width(countText));
            }
            itemsWidth = Math.max(drawerComponent.items().size() * SLOT_SIZE, maxTextWidth + SLOT_SIZE);
        }

        int networkWidth = 0;
        if (drawerComponent.networkKey() != null) {
            String access = drawerComponent.networkKey().isPublic() ? "Public" : "Personal";
            int swatchSize = 6;
            int swatchGap = 2;
            networkWidth = Math.max(font.width(access), 3 * swatchSize + 2 * swatchGap);
        }

        return Math.max(itemsWidth, networkWidth);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int networkBlockHeight = 0;

        if (drawerComponent.networkKey() != null) {
            WirelessNetworkKey key = drawerComponent.networkKey();

            String access = key.isPublic() ? "Public" : "Personal";
            graphics.text(font, access, x, y, 0xFFFFFFFF, false);

            int swatchY = y + font.lineHeight + 2;
            int[] colors = { key.color1(), key.color2(), key.color3() };
            for (int i = 0; i < colors.length; i++) {
                int sx = x + i * (SWATCH_SIZE + SWATCH_GAP);
                int color = 0xFF000000 | colors[i];
                graphics.fill(sx, swatchY, sx + SWATCH_SIZE, swatchY + SWATCH_SIZE, color);
            }

            networkBlockHeight = font.lineHeight + 2 + SWATCH_SIZE + 2;
        }

        for (int i = 0; i < drawerComponent.items().size(); i++) {
            ItemStack stack = drawerComponent.items().get(i);
            long count = drawerComponent.counts().get(i);
            int slotX = x + (i * SLOT_SIZE);
            int itemY = y + networkBlockHeight;

            graphics.item(stack, slotX + 1, itemY + 1);

            String countText = String.valueOf(count);
            graphics.text(font, countText, slotX + SLOT_SIZE - 2 - font.width(countText), itemY + SLOT_SIZE - 9, 0xFFFFFF, true);
        }
    }
}