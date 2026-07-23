package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FilingCabinetScreen extends AbstractContainerScreen<FilingCabinetMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/drawer_gui.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_WIDTH = 160;
    private static final int SEARCH_HEIGHT = 12;

    private static final int SCROLLBAR_X = 170;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_Y = FilingCabinetMenu.GRID_TOP;
    private static final int SCROLLBAR_HEIGHT = FilingCabinetMenu.VIEWPORT_HEIGHT;
    private static final int THUMB_MIN_HEIGHT = 12;

    private EditBox searchBox;
    private boolean isDraggingScroll = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        super.init();

        searchBox = new EditBox(this.font,
                this.leftPos + SEARCH_X, this.topPos + SEARCH_Y,
                SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.literal("Search..."));
        searchBox.setMaxLength(64);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setHint(Component.literal("Search...").withStyle(s -> s.withColor(0xFFAAAAAA)));
        searchBox.setResponder(text -> this.menu.setSearchText(text));
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.imageHeight - 94, -12566464, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int bx = this.leftPos;
        int by = this.topPos;

        graphics.fill(bx, by, bx + imageWidth, by + imageHeight, 0xFFC6C6C6);
        graphics.fill(bx + 2, by + 2, bx + imageWidth - 2, by + imageHeight - 2, 0xFF8B8B8B);

        graphics.fill(bx + SEARCH_X - 1, by + SEARCH_Y - 1,
                bx + SEARCH_X + SEARCH_WIDTH + 1, by + SEARCH_Y + SEARCH_HEIGHT + 1, 0xFF555555);

        int viewTop = by + FilingCabinetMenu.GRID_TOP;
        int viewBottom = viewTop + FilingCabinetMenu.VIEWPORT_HEIGHT;
        graphics.fill(bx + FilingCabinetMenu.GRID_LEFT - 1, viewTop - 1,
                bx + FilingCabinetMenu.GRID_LEFT + FilingCabinetMenu.COLS * FilingCabinetMenu.CELL_SIZE + 1,
                viewBottom + 1, 0xFF373737);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBorder(graphics, bx + 8 + col * 18, by + 140 + row * 18, 16);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBorder(graphics, bx + 8 + col * 18, by + 198, 16);
        }

        for (var header : menu.getHeaders()) {
            int hy = by + FilingCabinetMenu.GRID_TOP + header.y() - menu.getScrollOffset();

            if (hy < viewTop || hy + 11 > viewBottom) continue;

            graphics.fill(bx + FilingCabinetMenu.GRID_LEFT, hy,
                    bx + FilingCabinetMenu.GRID_LEFT + FilingCabinetMenu.COLS * FilingCabinetMenu.CELL_SIZE, hy + 11,
                    0xFF4A4A4A);
            graphics.text(this.font, Component.literal(header.category().displayName()),
                    bx + FilingCabinetMenu.GRID_LEFT + 2, hy + 2, 0xFFF2F3E5, false);
        }

        int maxScroll = menu.getMaxScrollOffset();
        if (maxScroll > 0) {
            int sbX = bx + SCROLLBAR_X;
            int sbY = by + SCROLLBAR_Y;
            graphics.fill(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + SCROLLBAR_HEIGHT, 0xFF555555);

            int thumbH = Math.max(THUMB_MIN_HEIGHT,
                    SCROLLBAR_HEIGHT * FilingCabinetMenu.VIEWPORT_HEIGHT / (menu.getContentHeightSafe()));
            int trackRange = SCROLLBAR_HEIGHT - thumbH;
            int thumbY = sbY + (maxScroll > 0 ? trackRange * menu.getScrollOffset() / maxScroll : 0);
            graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbH, 0xFFAAAAAA);
        }

        if (mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {

            java.util.List<Component> searchTooltip = new java.util.ArrayList<>();
            searchTooltip.add(Component.literal("Search").withStyle(s -> s.withBold(true)));
            searchTooltip.add(Component.literal("@ - Search by mod").withStyle(s -> s.withColor(0xFF888888)));
            searchTooltip.add(Component.literal("# - Search by tag").withStyle(s -> s.withColor(0xFF888888)));
            searchTooltip.add(Component.literal("$ - Search by tooltip").withStyle(s -> s.withColor(0xFF888888)));

            graphics.setTooltipForNextFrame(this.font, searchTooltip, java.util.Optional.empty(), net.minecraft.world.item.ItemStack.EMPTY, mouseX, mouseY);
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSlotBorder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + size, y, 0xFF555555);
        graphics.fill(x - 1, y - 1, x, y + size, 0xFF555555);
        graphics.fill(x, y + size, x + size + 1, y + size + 1, 0xFFFFFFFF);
        graphics.fill(x + size, y, x + size + 1, y + size + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.getMaxScrollOffset() > 0) {
            menu.setScrollOffset(menu.getScrollOffset() - (int) (scrollY * 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int sbX = this.leftPos + SCROLLBAR_X;
        int sbY = this.topPos + SCROLLBAR_Y;
        int maxScroll = menu.getMaxScrollOffset();
        if (maxScroll > 0 && event.x() >= sbX && event.x() <= sbX + SCROLLBAR_WIDTH
                && event.y() >= sbY && event.y() <= sbY + SCROLLBAR_HEIGHT) {
            isDraggingScroll = true;
            dragStartY = (int) event.y();
            dragStartOffset = menu.getScrollOffset();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isDraggingScroll && menu.getMaxScrollOffset() > 0) {
            int delta = (int) event.y() - dragStartY;
            int contentRange = Math.max(1, menu.getContentHeightSafe() - FilingCabinetMenu.VIEWPORT_HEIGHT);
            int newOffset = dragStartOffset + delta * contentRange / SCROLLBAR_HEIGHT;
            menu.setScrollOffset(newOffset);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (searchBox.keyPressed(event)) return true;
        if (searchBox.isFocused()) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox.charTyped(event)) return true;
        return super.charTyped(event);
    }
}