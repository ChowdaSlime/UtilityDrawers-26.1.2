package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidDrawerScreen extends AbstractContainerScreen<FluidDrawerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int UPGRADE_SLOT_X = 152;
    private static final int UPGRADE_SLOT_Y_START = 8;
    private static final int UPGRADE_SLOT_SIZE = 18;

    public FluidDrawerScreen(FluidDrawerMenu menu, Inventory playerInventory, Component title) {
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

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = bx + 8 + col * 18;
                int sy = by + 84 + row * 18;
                drawSlotBorder(graphics, sx, sy, 16);
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            int sx = bx + 8 + col * 18;
            int sy = by + 142;
            drawSlotBorder(graphics, sx, sy, 16);
        }

        // Upgrades (Items)
        for (int i = 0; i < 4; i++) {
            int sx = bx + UPGRADE_SLOT_X;
            int sy = by + UPGRADE_SLOT_Y_START + i * UPGRADE_SLOT_SIZE;
            drawSlotBorder(graphics, sx, sy, 16);
        }

        FluidDrawerBlockEntity drawer = this.menu.getBlockEntity();
        int slotCount = this.menu.getDrawerSlotCount();
        int[][] slotPositions = getSlotPositions(slotCount);

        for (int i = 0; i < slotCount; i++) {
            int slotX = bx + slotPositions[i][0];
            int slotY = by + slotPositions[i][1];
            int slotSize = slotPositions[i][2];

            drawSlotBorder(graphics, slotX, slotY, slotSize - 2);

            if (!drawer.isSlotEmpty(i)) {
                FluidStack stack = drawer.getStoredFluid(i);
                long amount = drawer.getStoredAmount(i);

                int fluidX = slotX;
                int fluidY = slotY;
                int fluidFillSize = slotSize - 2;

                drawFluid(graphics, stack, fluidX, fluidY, fluidFillSize, fluidFillSize);

                if (mouseX >= slotX && mouseX < slotX + slotSize
                        && mouseY >= slotY && mouseY < slotY + slotSize) {
                    graphics.setTooltipForNextFrame(this.font,
                            Component.literal(stack.getHoverName().getString() + " - " + amount + " mB"),
                            mouseX, mouseY);
                }
            }
        }

        super.extractContents(graphics, mouseX, mouseY, a);
    }

    private void drawFluid(GuiGraphicsExtractor graphics, FluidStack stack, int x, int y, int width, int height) {
        if (stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();

        BlockState fluidState = stack.getFluid().defaultFluidState().createLegacyBlock();

        TextureAtlasSprite sprite = mc.getModelManager()
                .getBlockStateModelSet()
                .get(fluidState)
                .particleMaterial()
                .sprite();

        int tint = -1;
        BlockColors blockColors = mc.getBlockColors();
        BlockTintSource source = blockColors.getTintSource(fluidState, 0);
        if (source != null && mc.level != null && mc.player != null) {
            tint = source.colorInWorld(fluidState, mc.level, mc.player.blockPosition());
        }

        int fluidColor = tint == -1
                ? (stack.getFluid() == Fluids.WATER ? 0xFF3F76E4 : 0xFFFFFFFF)
                : (0xFF000000 | tint);


        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, fluidColor);
    }

    private void drawSlotBorder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + size, y, 0xFF555555);
        graphics.fill(x - 1, y - 1, x, y + size, 0xFF555555);
        graphics.fill(x, y + size, x + size + 1, y + size + 1, 0xFFFFFFFF);
        graphics.fill(x + size, y, x + size + 1, y + size + 1, 0xFFFFFFFF);
    }

    private int[][] getSlotPositions(int slotCount) {
        int size = 20;
        return switch (slotCount) {
            case 1 -> new int[][]{{78, 30, size}};
            case 2 -> new int[][]{{78, 20, size}, {78, 42, size}};
            case 3 -> new int[][]{{78, 18, size}, {66, 40, size}, {90, 40, size}};
            case 4 -> new int[][]{{66, 18, size}, {90, 18, size}, {66, 40, size}, {90, 40, size}};
            default -> new int[][]{{78, 30, size}};
        };
    }
}