package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DrawerScreen<T extends DrawerMenu> extends AbstractContainerScreen<T> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int UPGRADE_SLOT_X = 152;
    private static final int UPGRADE_SLOT_Y_START = 8;
    private static final int UPGRADE_SLOT_SIZE = 18;

    public DrawerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        super.init();
    }


    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.imageHeight - 94, -12566464, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int bx = this.leftPos;
        int by = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                bx, by, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = bx + 8 + col * 18;
                int sy = by + 84 + row * 18;
                drawSlotBorder(graphics, sx, sy, 16);
            }
        }

        for (int col = 0; col < 9; col++) {
            int sx = bx + 8 + col * 18;
            int sy = by + 142;
            drawSlotBorder(graphics, sx, sy, 16);
        }

        if (this.menu.hasUpgrades()) {
            for (int i = 0; i < 4; i++) {
                int sx = bx + UPGRADE_SLOT_X;
                int sy = by + UPGRADE_SLOT_Y_START + i * UPGRADE_SLOT_SIZE;
                drawSlotBorder(graphics, sx, sy, 16);
            }
        }

        DrawerBlockEntity drawer = this.menu.getBlockEntity();
        int slotCount = this.menu.getDrawerSlotCount();
        int[][] slotPositions = getSlotPositions(slotCount);

        for (int i = 0; i < slotCount; i++) {
            int slotX = bx + slotPositions[i][0];
            int slotY = by + slotPositions[i][1];
            int slotSize = slotPositions[i][2];

            drawSlotBorder(graphics, slotX, slotY, slotSize - 2);

            if (!drawer.isSlotEmpty(i)) {
                ItemStack stack = drawer.getStoredItem(i);
                int itemX = slotX + slotSize / 2 - 9;
                int itemY = slotY + slotSize / 2 - 9;
                graphics.item(stack, itemX, itemY);



                if (mouseX >= slotX && mouseX < slotX + slotSize
                        && mouseY >= slotY && mouseY < slotY + slotSize) {
                    graphics.setTooltipForNextFrame(this.font,
                            Component.literal(stack.getHoverName().getString()
                                    + " x" + drawer.getStoredCount(i)),
                            mouseX, mouseY);
                }
            }
        }

        super.extractContents(graphics, mouseX, mouseY, a);
    }

    private void drawSlotBorder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + size, y, 0xFF555555);
        graphics.fill(x - 1, y - 1, x, y + size, 0xFF555555);
        graphics.fill(x, y + size, x + size + 1, y + size + 1, 0xFFFFFFFF);
        graphics.fill(x + size, y, x + size + 1, y + size + 1, 0xFFFFFFFF);
    }

    private int[][] getSlotPositions(int slotCount) {
        return switch (slotCount) {
            case 1 -> new int[][]{
                    {75, 28, 24}
            };
            case 2 -> new int[][]{
                    {75, 17, 24},
                    {75, 43, 24}
            };
            case 3 -> new int[][]{
                    {75, 12, 24},
                    {62, 37, 24},
                    {88, 37, 24}
            };
            case 4 -> new int[][]{
                    {62, 12, 24},
                    {88, 12, 24},
                    {62, 37, 24},
                    {88, 37, 24}
            };
            default -> new int[][]{{75, 28, 24}};
        };
    }
}