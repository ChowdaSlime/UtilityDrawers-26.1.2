package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class DrawerFramerScreen extends AbstractContainerScreen<DrawerFramerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_framer_gui.png");
    private static final int GUI_WIDTH = 512;
    private static final int GUI_HEIGHT = 490;
    private static final float UI_SCALE = 0.5f;

    public DrawerFramerScreen(DrawerFramerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, (int)(GUI_WIDTH * UI_SCALE), (int)(GUI_HEIGHT * UI_SCALE));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        super.init();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int bx = this.leftPos;
        int by = this.topPos;

        graphics.pose().pushMatrix();
        graphics.pose().translate(bx, by);
        graphics.pose().scale(UI_SCALE, UI_SCALE);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                0, 0, 0, 22,
                GUI_WIDTH, GUI_HEIGHT, 1024, 1024);

        graphics.pose().popMatrix();

        int progress = menu.getProgress();
        int max = menu.getMaxProgress();

        if (max > 0) {

            int arrowTexX = 553;
            int arrowTexY = 212;
            int fullArrowWidth = 171;
            int arrowHeight = 57;

            int filledWidth = progress * fullArrowWidth / max;

            if (filledWidth > 0) {
                int arrowOffsetX = 86;
                int arrowOffsetY = 61;

                graphics.pose().pushMatrix();
                graphics.pose().translate(bx + arrowOffsetX, by + arrowOffsetY);
                graphics.pose().scale(UI_SCALE, UI_SCALE);

                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                        0, 0,
                        arrowTexX, arrowTexY,
                        filledWidth, arrowHeight,
                        1024, 1024);

                graphics.pose().popMatrix();
            }
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
}