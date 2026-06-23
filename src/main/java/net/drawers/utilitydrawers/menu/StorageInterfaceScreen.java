package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int UPGRADE_SLOT_X = 152;
    private static final int UPGRADE_SLOT_Y = 35;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory playerInventory, Component title) {
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
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.imageHeight - 94, -12566464, false);
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
                drawSlotBorder(graphics, bx + 8 + col * 18, by + 84 + row * 18, 16);
            }
        }

        for (int col = 0; col < 9; col++) {
            drawSlotBorder(graphics, bx + 8 + col * 18, by + 142, 16);
        }

        drawSlotBorder(graphics, bx + UPGRADE_SLOT_X, by + UPGRADE_SLOT_Y, 16);

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