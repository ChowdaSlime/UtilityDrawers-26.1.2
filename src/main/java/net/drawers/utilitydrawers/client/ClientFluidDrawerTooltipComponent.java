package net.drawers.utilitydrawers.client;

import net.drawers.utilitydrawers.data.WirelessNetworkKey;
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
    private static final int SWATCH_SIZE = 6;
    private static final int SWATCH_GAP = 2;

    public ClientFluidDrawerTooltipComponent(FluidDrawerTooltipComponent fluidComponent) {
        this.fluidComponent = fluidComponent;
    }

    @Override
    public int getHeight(Font font) {
        int height = 0;

        if (fluidComponent.networkKey() != null) {
            height += font.lineHeight + 2 + SWATCH_SIZE + 2;
        }

        if (!fluidComponent.fluids().isEmpty()) {
            height += SLOT_SIZE;
        }

        return height;
    }

    @Override
    public int getWidth(Font font) {
        int fluidsWidth = 0;
        if (!fluidComponent.fluids().isEmpty()) {
            int maxTextWidth = 0;
            for (int i = 0; i < fluidComponent.amounts().size(); i++) {
                String amountText = formatMillibuckets(fluidComponent.amounts().get(i));
                maxTextWidth = Math.max(maxTextWidth, font.width(amountText));
            }
            fluidsWidth = Math.max(fluidComponent.fluids().size() * SLOT_SIZE, maxTextWidth + SLOT_SIZE);
        }

        int networkWidth = 0;
        if (fluidComponent.networkKey() != null) {
            String access = fluidComponent.networkKey().isPublic() ? "Public" : "Personal";
            networkWidth = Math.max(font.width(access), 3 * SWATCH_SIZE + 2 * SWATCH_GAP);
        }

        return Math.max(fluidsWidth, networkWidth);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int networkBlockHeight = 0;

        if (fluidComponent.networkKey() != null) {
            WirelessNetworkKey key = fluidComponent.networkKey();

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

        for (int i = 0; i < fluidComponent.fluids().size(); i++) {
            FluidStack stack = fluidComponent.fluids().get(i);
            long amount = fluidComponent.amounts().get(i);
            int slotX = x + (i * SLOT_SIZE);
            int slotY = y + networkBlockHeight;

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

                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, slotX + 1, slotY + 1, 16, 16, fluidColor);
            }

            String amountText = formatMillibuckets(amount);
            graphics.text(font, amountText, slotX + SLOT_SIZE - 2 - font.width(amountText), slotY + SLOT_SIZE - 9, 0xFFFFFF, true);
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