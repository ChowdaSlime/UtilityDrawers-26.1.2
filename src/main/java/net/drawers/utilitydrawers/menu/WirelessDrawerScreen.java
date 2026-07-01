package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.data.WirelessNetworkKey;
import net.drawers.utilitydrawers.network.UpdateWirelessDrawerPayload;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public class WirelessDrawerScreen extends DrawerScreen<WirelessDrawerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/wireless_drawer_gui.png");

    private static final int SETTINGS_X1 = 157;
    private static final int SETTINGS_Y1 = 4;
    private static final int SETTINGS_X2 = 171;
    private static final int SETTINGS_Y2 = 18;
    private static final int SETTINGS_W = SETTINGS_X2 - SETTINGS_X1;
    private static final int SETTINGS_H = SETTINGS_Y2 - SETTINGS_Y1;

    private static final int TEX_SETTINGS_U = 157;
    private static final int TEX_SETTINGS_V = 4;

    private static final int PERSONAL_X1 = 180;
    private static final int PERSONAL_Y1 = 10;
    private static final int PERSONAL_X2 = 234;
    private static final int PERSONAL_Y2 = 23;
    private static final int PERSONAL_W = PERSONAL_X2 - PERSONAL_X1;
    private static final int PERSONAL_H = PERSONAL_Y2 - PERSONAL_Y1;

    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 4;
    private static final int SWATCH_ROW_Y = 27;
    private static final int SWATCH_START_X = 182;

    private static final int TEX_TOGGLE_U = 187;
    private static final int TEX_PERSONAL_ACTIVE_V = 10;
    private static final int TEX_PUBLIC_ACTIVE_V = 25;

    private boolean settingsOpen = false;

    public WirelessDrawerScreen(WirelessDrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleColor = -12566464;
        graphics.text(this.font,
                Component.literal("Wireless"), this.titleLabelX, this.titleLabelY, titleColor, false);
        graphics.text(this.font,
                Component.literal("Drawer"), this.titleLabelX, this.titleLabelY + this.font.lineHeight, titleColor, false);

        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.imageHeight - 94, titleColor, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        boolean isRightClick = event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.button() == 1;

        if (isInBox(mouseX, mouseY, SETTINGS_X1, SETTINGS_Y1, SETTINGS_X2, SETTINGS_Y2)) {
            settingsOpen = !settingsOpen;
            return true;
        }

        if (settingsOpen) {
            if (isInBox(mouseX, mouseY, PERSONAL_X1, PERSONAL_Y1, PERSONAL_X2, PERSONAL_Y2)) {
                getWirelessMenu().togglePublic();
                sendUpdatePacket();
                return true;
            }

            for (int i = 0; i < 3; i++) {
                int bx = this.leftPos + SWATCH_START_X + i * (SWATCH_SIZE + SWATCH_GAP);
                int by = this.topPos + SWATCH_ROW_Y;

                if (mouseX >= bx && mouseX <= bx + SWATCH_SIZE && mouseY >= by && mouseY <= by + SWATCH_SIZE) {
                    getWirelessMenu().cycleColor(i, isRightClick);
                    sendUpdatePacket();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean isInBox(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        int ax1 = this.leftPos + x1;
        int ay1 = this.topPos + y1;
        int ax2 = this.leftPos + x2;
        int ay2 = this.topPos + y2;
        return mouseX >= ax1 && mouseX <= ax2 && mouseY >= ay1 && mouseY <= ay2;
    }

    private void sendUpdatePacket() {
        WirelessNetworkKey newKey = getWirelessMenu().buildCurrentKey();
        ClientPacketDistributor.sendToServer(
                new UpdateWirelessDrawerPayload(getWirelessMenu().getBlockPos(), newKey)
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        int sx = this.leftPos + SETTINGS_X1;
        int sy = this.topPos + SETTINGS_Y1;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                sx, sy, TEX_SETTINGS_U, TEX_SETTINGS_V,
                SETTINGS_W, SETTINGS_H, 256, 256);

        if (!settingsOpen) {
            return;
        }

        WirelessDrawerMenu wMenu = getWirelessMenu();
        boolean isPublic = wMenu.isPublic();

        int toggleX = this.leftPos + PERSONAL_X1;
        int toggleY = this.topPos + PERSONAL_Y1;
        int toggleV = isPublic ? TEX_PUBLIC_ACTIVE_V : TEX_PERSONAL_ACTIVE_V;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                toggleX, toggleY, TEX_TOGGLE_U, toggleV,
                PERSONAL_W, PERSONAL_H, 256, 256);

        for (int i = 0; i < 3; i++) {
            int bx = this.leftPos + SWATCH_START_X + i * (SWATCH_SIZE + SWATCH_GAP);
            int by = this.topPos + SWATCH_ROW_Y;
            int color = 0xFF000000 | WirelessDrawerMenu.NETWORK_COLORS[wMenu.getColorIndex(i)];

            graphics.fill(bx + 2, by + 2, bx + SWATCH_SIZE - 2, by + SWATCH_SIZE - 2, color);
            graphics.fill(bx + 1, by + 1, bx + SWATCH_SIZE - 1, by + 2, 0xFFFFFFFF);
            graphics.fill(bx + 1, by + 1, bx + 2, by + SWATCH_SIZE - 1, 0xFFFFFFFF);
            graphics.fill(bx + 1, by + SWATCH_SIZE - 2, bx + SWATCH_SIZE - 1, by + SWATCH_SIZE - 1, 0xFFFFFFFF);
            graphics.fill(bx + SWATCH_SIZE - 2, by + 1, bx + SWATCH_SIZE - 1, by + SWATCH_SIZE - 1, 0xFFFFFFFF);
        }

        String keyStr = buildKeyPreview(wMenu);
        graphics.text(this.font,
                Component.literal(keyStr),
                this.leftPos + SWATCH_START_X - 10, this.topPos + SWATCH_ROW_Y + SWATCH_SIZE + 4,
                0xCCCCCC, false);
    }

    private String buildKeyPreview(WirelessDrawerMenu menu) {
        String c1 = String.format("#%06X", WirelessDrawerMenu.NETWORK_COLORS[menu.getColorIndex(0)]);
        String c2 = String.format("#%06X", WirelessDrawerMenu.NETWORK_COLORS[menu.getColorIndex(1)]);
        String c3 = String.format("#%06X", WirelessDrawerMenu.NETWORK_COLORS[menu.getColorIndex(2)]);
        return c1 + "-" + c2 + "-" + c3;
    }

    private WirelessDrawerMenu getWirelessMenu() {
        return this.menu;
    }
}