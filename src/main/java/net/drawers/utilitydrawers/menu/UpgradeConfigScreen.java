package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class UpgradeConfigScreen extends AbstractContainerScreen<UpgradeConfigMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int[][] DIRECTION_LAYOUT = {
            {Direction.DOWN.get3DDataValue(), Direction.EAST.get3DDataValue()},
            {Direction.UP.get3DDataValue(), Direction.SOUTH.get3DDataValue()},
            {Direction.NORTH.get3DDataValue(), Direction.WEST.get3DDataValue()}
    };

    public UpgradeConfigScreen(UpgradeConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        int bx = this.leftPos;
        int by = this.topPos;

        for (int row = 0; row < DIRECTION_LAYOUT.length; row++) {
            for (int col = 0; col < DIRECTION_LAYOUT[row].length; col++) {

                final int tabId = DIRECTION_LAYOUT[row][col];
                Direction dir = Direction.from3DDataValue(tabId);

                this.addRenderableWidget(
                        Button.builder(Component.literal(dir.name().substring(0, 1)), btn ->
                                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tabId))
                                        .bounds(bx + 7 + col * 22, by + 16 + row * 18, 16, 16).build());
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Toggle"), btn -> {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 10);})
                .bounds(bx + 120, by + 34, 40, 20).build());
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, by, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBorder(graphics, bx + 8 + col * 18, by + 84 + row * 18, 16);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBorder(graphics, bx + 8 + col * 18, by + 142, 16);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBorder(graphics, bx + 62 + col * 18, by + 17 + row * 18, 16);
            }
        }

        boolean isActive = this.menu.isSideActive(this.menu.getCurrentTab());
        int color = isActive ? 0xFF00FF00 : 0xFFFF0000;
        String statusText = isActive ? "ACTIVE" : "DISABLED";

        graphics.text(this.font, Component.literal(statusText), bx + 120, by + 20, color, true);

        int activeRow = 0;
        int activeCol = 0;

        for (int row = 0; row < DIRECTION_LAYOUT.length; row++) {
            for (int col = 0; col < DIRECTION_LAYOUT[row].length; col++) {
                if (DIRECTION_LAYOUT[row][col] == this.menu.getCurrentTab()) {
                    activeRow = row;
                    activeCol = col;
                }
            }
        }
        int x = bx + 7 + activeCol * 22;
        int y = by + 16 + activeRow * 18;

        graphics.fill(x - 2, y - 2, x + 18, y + 18, 0x44FFFFFF);

        super.extractContents(graphics, mouseX, mouseY, a);
    }

    private void drawSlotBorder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + size, y, 0xFF555555);
        graphics.fill(x - 1, y - 1, x, y + size, 0xFF555555);
        graphics.fill(x, y + size, x + size + 1, y + size + 1, 0xFFFFFFFF);
        graphics.fill(x + size, y, x + size + 1, y + size + 1, 0xFFFFFFFF);
    }
}