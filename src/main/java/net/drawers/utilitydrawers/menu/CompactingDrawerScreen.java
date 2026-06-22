package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CompactingDrawerScreen extends AbstractContainerScreen<CompactingDrawerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int UPGRADE_SLOT_X      = 152;
    private static final int UPGRADE_SLOT_Y_START = 8;
    private static final int UPGRADE_SLOT_SIZE    = 18;

    public CompactingDrawerScreen(CompactingDrawerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width  - this.imageWidth)  / 2;
        this.topPos  = (this.height - this.imageHeight) / 2;
        super.init();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleColor = -12566464;
        graphics.text(this.font,
                Component.literal("Compacting"), this.titleLabelX, this.titleLabelY, titleColor, false);
        graphics.text(this.font,
                Component.literal("Drawer"), this.titleLabelX, this.titleLabelY + this.font.lineHeight, titleColor, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.imageHeight - 94, titleColor, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int bx = this.leftPos;
        int by = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                bx, by, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBorder(graphics, bx + 8 + col * 18, by + 84 + row * 18, 16);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBorder(graphics, bx + 8 + col * 18, by + 142, 16);
        }

        for (int i = 0; i < 4; i++) {
            drawSlotBorder(graphics, bx + UPGRADE_SLOT_X, by + UPGRADE_SLOT_Y_START + i * UPGRADE_SLOT_SIZE, 16);
        }

        int[][] slotPositions = {
                {75, 12, 24},
                {62, 37, 24},
                {88, 37, 24},
        };

        CompactingDrawerBlockEntity drawer = this.menu.getBlockEntity();

        for (int i = 0; i < 3; i++) {
            int slotX    = bx + slotPositions[i][0];
            int slotY    = by + slotPositions[i][1];
            int slotSize =      slotPositions[i][2];

            drawSlotBorder(graphics, slotX, slotY, slotSize - 2);

            if (!drawer.isSlotEmpty(i)) {
                ItemStack stack = drawer.getStoredItem(i);
                int itemX = slotX + slotSize / 2 - 9;
                int itemY = slotY + slotSize / 2 - 9;
                graphics.item(stack, itemX, itemY);

                long count = drawer.getStoredCount(i);

                if (mouseX >= slotX && mouseX < slotX + slotSize
                        && mouseY >= slotY && mouseY < slotY + slotSize) {
                    graphics.setTooltipForNextFrame(this.font,
                            Component.literal(stack.getHoverName().getString() + " x" + count),
                            mouseX, mouseY);
                }
            }
        }

        super.extractContents(graphics, mouseX, mouseY, a);
    }

    private void drawSlotBorder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x,     y,     x + size,     y + size,     0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + size,     y,            0xFF555555);
        graphics.fill(x - 1, y - 1, x,            y + size,     0xFF555555);
        graphics.fill(x,     y + size, x + size + 1, y + size + 1, 0xFFFFFFFF);
        graphics.fill(x + size, y,   x + size + 1, y + size + 1, 0xFFFFFFFF);
    }
}