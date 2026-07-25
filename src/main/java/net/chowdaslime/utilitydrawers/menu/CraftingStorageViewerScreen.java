package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CraftingStorageViewerScreen extends StorageViewerScreen {

    private static final Identifier CRAFTING_TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/crafting_storage_viewer_gui.png");

    private static final int TEX_SORT_COUNT_U = 180;
    private static final int TEX_SORT_NAME_U = 203;
    private static final int TEX_SORT_TYPE_V = 107;

    private static final int TEX_SORT_ASC_U = 180;
    private static final int TEX_SORT_DESC_U = 203;
    private static final int TEX_SORT_DIR_V = 130;

    private static final int TEX_BUTTON_SIZE_1_U = 180;
    private static final int TEX_BUTTON_SIZE_2_U = 203;
    private static final int TEX_BUTTON_SIZE_3_U = 226;
    private static final int TEX_BUTTON_SIZE_V = 153;

    private static final int TEX_BUTTON_JEI_U = 180;
    private static final int TEX_BUTTON_JEI_V = 177;
    private static final int BUTTON_SIZE = 20;

    public CraftingStorageViewerScreen(StorageViewerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public int getImageHeight() {
        return 202 + (getMenu().viewerRows * 18);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int bx = this.leftPos;
        int by = this.topPos;
        int topHeight = 17;

        int bottomV = 71;
        int bottomHeight = 184;

        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE, bx, by, 0, 0, 195, topHeight, 256, 256);

        for (int r = 0; r < getMenu().viewerRows; r++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE, bx, by + topHeight + (r * 18), 0, 17, 195, 18, 256, 256);
        }

        int bottomY = by + topHeight + (getMenu().viewerRows * 18);

        int upperBottomHeight = 107 - bottomV;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE, bx, bottomY, 0, bottomV, 195, upperBottomHeight, 256, 256);

        int lowerBottomHeight = bottomHeight - upperBottomHeight;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE, bx, bottomY + upperBottomHeight, 0, bottomV + upperBottomHeight, 179, lowerBottomHeight, 256, 256);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        int bx = this.leftPos;
        int by = this.topPos;
        int btnX = bx - 20;

        int sortTypeY = by + 9;
        int sortTypeU = getMenu().sortByCount ? TEX_SORT_COUNT_U : TEX_SORT_NAME_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE,
                btnX, sortTypeY, sortTypeU, TEX_SORT_TYPE_V,
                BUTTON_SIZE, BUTTON_SIZE, 256, 256);

        int sortDirY = by + 29;
        int sortDirU = getMenu().sortAscending ? TEX_SORT_ASC_U : TEX_SORT_DESC_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE,
                btnX, sortDirY, sortDirU, TEX_SORT_DIR_V,
                BUTTON_SIZE, BUTTON_SIZE, 256, 256);

        int sizeY = by + 49;
        int sizeU = getMenu().viewerRows == 3 ? TEX_BUTTON_SIZE_1_U : getMenu().viewerRows == 6 ? TEX_BUTTON_SIZE_2_U : TEX_BUTTON_SIZE_3_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE,
                btnX, sizeY, sizeU, TEX_BUTTON_SIZE_V,
                BUTTON_SIZE, BUTTON_SIZE, 256, 256);

        int jeiY = by + 70;

        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TEXTURE,
                btnX, jeiY, TEX_BUTTON_JEI_U, TEX_BUTTON_JEI_V,
                BUTTON_SIZE, 22, 256, 256);

        if (getMenu().syncJei) {
            graphics.outline(btnX, jeiY, BUTTON_SIZE, 22, 0xFFE0E0E0);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int yOffset = (getMenu().viewerRows - 3) * 18;

        graphics.text(this.font, Component.translatable("container.crafting"),
                this.titleLabelX, 82 + yOffset, 0xFFF2F3E5, false);

        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, 156 + yOffset, 0xFFF2F3E5, false);
    }
}