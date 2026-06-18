package net.drawers.utilitydrawers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class ClientFluidDrawerTooltipComponent implements ClientTooltipComponent {

    private final FluidDrawerTooltipComponent fluidComponent;
    private static final int SLOT_SIZE = 20;

    public ClientFluidDrawerTooltipComponent(FluidDrawerTooltipComponent fluidComponent) {
        this.fluidComponent = fluidComponent;
    }

    @Override
    public int getHeight(Font font) {
        return SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        int maxTextWidth = 0;
        for (int i = 0; i < fluidComponent.amounts().size(); i++) {
            String amountText = formatMillibuckets(fluidComponent.amounts().get(i));
            maxTextWidth = Math.max(maxTextWidth, font.width(amountText));
        }
        return Math.max(fluidComponent.fluids().size() * SLOT_SIZE, maxTextWidth + SLOT_SIZE);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        for (int i = 0; i < fluidComponent.fluids().size(); i++) {
            FluidStack stack = fluidComponent.fluids().get(i);
            long amount = fluidComponent.amounts().get(i);
            int slotX = x + (i * SLOT_SIZE);

            if (!stack.isEmpty()) {
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

                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, slotX + 1, y + 1, 16, 16, fluidColor);
            }

            String amountText = formatMillibuckets(amount);
            graphics.text(font, amountText, slotX + SLOT_SIZE - 2 - font.width(amountText), y + SLOT_SIZE - 9, 0xFFFFFF, true);
        }
    }

    private static String formatMillibuckets(long mb) {
        if (mb >= 1000) {
            long whole = mb / 1000;
            long remainder = (mb % 1000) / 100;
            return remainder == 0 ? whole + " B" : whole + "." + remainder + " B";
        }
        return mb + " mB";
    }
}