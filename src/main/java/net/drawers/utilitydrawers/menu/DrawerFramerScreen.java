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

    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 490;
    private static final float UI_SCALE = 0.5f;

    public DrawerFramerScreen(DrawerFramerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, (int)(TEXTURE_WIDTH * UI_SCALE), (int)(TEXTURE_HEIGHT * UI_SCALE));
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
                TEXTURE_WIDTH, TEXTURE_HEIGHT, 512, 512);

        graphics.pose().popMatrix();

        int progress = menu.getProgress();
        int max = menu.getMaxProgress();

        if (max > 0) {
            int fullTextureWidth = 175;
            int filledTextureWidth = progress * fullTextureWidth / max;

            graphics.pose().pushMatrix();

            graphics.pose().translate(bx + 75, by + 54);
            graphics.pose().scale(UI_SCALE, UI_SCALE);

            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    0, 0, 170, 159, filledTextureWidth, 28, 512, 512);

            graphics.pose().popMatrix();
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
}