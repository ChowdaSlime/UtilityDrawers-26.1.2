package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.network.StorageViewerExtractPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StorageViewerScreen extends AbstractContainerScreen<StorageViewerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/storage_viewer_gui.png");

    private static final int GUI_WIDTH = 195;
    private static final int GUI_HEIGHT = 171;

    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 3;
    private static final int GRID_CELL_SIZE = 18;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 18;

    private static final int SCROLLBAR_X = 178;
    private static final int SCROLLBAR_Y = 19;
    private static final int SCROLLBAR_HEIGHT = 52;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int THUMB_HEIGHT = 15;

    private static final int SEARCH_X = 41;
    private static final int SEARCH_Y = 5;
    private static final int SEARCH_WIDTH = 102;
    private static final int SEARCH_HEIGHT = 12;

    private static final int SORT_BUTTON_X = -20;
    private static final int SORT_BUTTON_Y = 10;
    private static final int SORT_BUTTON_SIZE = 16;

    private EditBox searchBox;
    private int scrollOffset = 0;
    private boolean isDraggingScroll = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;
    private int hoveredCell = -1;

    private enum SortMode { NAME, COUNT }
    private SortMode sortMode = SortMode.NAME;

    private List<StorageViewerMenu.NetworkSlot> filteredSlots = new ArrayList<>();

    public StorageViewerScreen(StorageViewerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public int getImageWidth() { return GUI_WIDTH; }

    @Override
    public int getImageHeight() { return GUI_HEIGHT; }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        searchBox = new EditBox(this.font,
                this.leftPos + SEARCH_X, this.topPos + SEARCH_Y,
                SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.literal("Search..."));
        searchBox.setMaxLength(64);
        searchBox.setBordered(false);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFF2F3E5);
        searchBox.setHint(Component.literal("Search...").withStyle(s -> s.withColor(0xFFF2F3E5)));
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            rebuildFilteredSlots();
        });
        this.addRenderableWidget(this.searchBox);

        rebuildFilteredSlots();
    }

    public void rebuildFilteredSlots() {
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase().trim();
        List<StorageViewerMenu.NetworkSlot> all = menu.getNetworkSlots();

        Comparator<StorageViewerMenu.NetworkSlot> comparator = !StorageViewerMenu.sortByCount
                ? Comparator.comparing((StorageViewerMenu.NetworkSlot ns) ->
                ns.stack().getHoverName().getString().toLowerCase())
                : Comparator.comparingLong(StorageViewerMenu.NetworkSlot::count).reversed();

        filteredSlots = all.stream()
                .filter(ns -> query.isEmpty() ||
                        ns.stack().getHoverName().getString().toLowerCase().contains(query))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private int totalRows() {
        return (int) Math.ceil((double) filteredSlots.size() / GRID_COLS);
    }

    private int maxScrollOffset() {
        return Math.max(0, totalRows() - GRID_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScrollOffset();
        if (max > 0) {
            scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int sbX = this.leftPos + SCROLLBAR_X;
        int thumbY = getThumbY();

        if (!isMouseOverSearchBox(mouseX, mouseY)) {
            searchBox.setFocused(false);
        }

        if (mouseX >= sbX && mouseX <= sbX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY <= thumbY + THUMB_HEIGHT) {
            isDraggingScroll = true;
            dragStartY = (int) mouseY;
            dragStartOffset = scrollOffset;
            return true;
        }

        int sortX = this.leftPos + SORT_BUTTON_X;
        int sortY = this.topPos + SORT_BUTTON_Y;

        if (mouseX >= sortX && mouseX <= sortX + SORT_BUTTON_SIZE
                && mouseY >= sortY && mouseY <= sortY + SORT_BUTTON_SIZE) {
            StorageViewerMenu.sortByCount = !StorageViewerMenu.sortByCount;
            rebuildFilteredSlots();
            return true;
        }

        int cellIndex = getCellAt((int) mouseX, (int) mouseY);
        if (cellIndex >= 0 && cellIndex < filteredSlots.size()) {
            StorageViewerMenu.NetworkSlot slot = filteredSlots.get(cellIndex);
            int amount = (button == 0) ? slot.stack().getMaxStackSize() : 1;
            ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                    slot.stack().copyWithCount(1),
                    amount,
                    slot.sources()
            ));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isDraggingScroll && maxScrollOffset() > 0) {
            int delta = (int) event.y() - dragStartY;
            int trackHeight = SCROLLBAR_HEIGHT - THUMB_HEIGHT;
            int newOffset = dragStartOffset
                    + (int) Math.round((double) delta / trackHeight * maxScrollOffset());
            scrollOffset = Math.max(0, Math.min(maxScrollOffset(), newOffset));
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
        if (searchBox.isFocused() && searchBox.isVisible()) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private int getThumbY() {
        int max = maxScrollOffset();
        int trackHeight = SCROLLBAR_HEIGHT - THUMB_HEIGHT;
        int thumbOffset = max > 0 ? (int) Math.round((double) scrollOffset / max * trackHeight) : 0;
        return this.topPos + SCROLLBAR_Y + thumbOffset;
    }

    private int getCellAt(int mouseX, int mouseY) {
        int gx = this.leftPos + GRID_LEFT;
        int gy = this.topPos + GRID_TOP;
        if (mouseX < gx || mouseX >= gx + GRID_COLS * GRID_CELL_SIZE) return -1;
        if (mouseY < gy || mouseY >= gy + GRID_ROWS * GRID_CELL_SIZE) return -1;
        int col = (mouseX - gx) / GRID_CELL_SIZE;
        int row = (mouseY - gy) / GRID_CELL_SIZE;
        return (scrollOffset + row) * GRID_COLS + col;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.leftPos, this.topPos, 0, 0,
                GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.menu.clientNeedsRebuild) {
            this.rebuildFilteredSlots();
            this.menu.clientNeedsRebuild = false;
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredCell >= 0 && hoveredCell < filteredSlots.size()) {
            StorageViewerMenu.NetworkSlot ns = filteredSlots.get(hoveredCell);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(ns.stack().getHoverName());
            tooltip.add(Component.literal("Count: " + ns.count())
                    .withStyle(s -> s.withColor(0xFFAAAAAA)));
            tooltip.add(Component.literal("Left-click: extract stack")
                    .withStyle(s -> s.withColor(0xFF888888)));
            tooltip.add(Component.literal("Right-click: extract 1")
                    .withStyle(s -> s.withColor(0xFF888888)));
            graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        int bx = this.leftPos;
        int by = this.topPos;

        int sortX = bx - 20;
        int sortY = by + 10;

        graphics.fill(sortX, sortY, sortX + SORT_BUTTON_SIZE, sortY + SORT_BUTTON_SIZE, 0xFF000000);
        graphics.fill(sortX + 1, sortY + 1, sortX + SORT_BUTTON_SIZE - 1, sortY + SORT_BUTTON_SIZE - 1, 0xFFC6C6C6);

        String sortLbl = (StorageViewerMenu.sortByCount) ? "#" : "N";
        graphics.text(this.font, Component.literal(sortLbl),
                sortX + SORT_BUTTON_SIZE / 2 - this.font.width(sortLbl) / 2,
                sortY + SORT_BUTTON_SIZE / 2 - this.font.lineHeight / 2,
                0xFF404040, false);

        int gx = bx + GRID_LEFT;
        int gy = by + GRID_TOP;
        hoveredCell = getCellAt(mouseX, mouseY);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int cellIndex = (scrollOffset + row) * GRID_COLS + col;
                int cx = gx + col * GRID_CELL_SIZE;
                int cy = gy + row * GRID_CELL_SIZE;

                if (cellIndex < filteredSlots.size()) {
                    StorageViewerMenu.NetworkSlot ns = filteredSlots.get(cellIndex);

                    graphics.item(ns.stack().copyWithCount(1), cx, cy);

                    String countStr = formatCount(ns.count());
                    int textX = cx + 16 - this.font.width(countStr);
                    int textY = cy + 16 - this.font.lineHeight + 1;

                    graphics.nextStratum();
                    graphics.text(this.font, Component.literal(countStr), textX, textY, 0xFFFFFFFF, true);
                }

                if (cellIndex == hoveredCell && cellIndex < filteredSlots.size()) {
                    graphics.fill(cx, cy, cx + 16, cy + 16, 0x80FFFFFF);
                }
            }
        }

        if (maxScrollOffset() > 0) {
            int sbX = bx + SCROLLBAR_X;
            int thumbY = getThumbY();
            graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + THUMB_HEIGHT, 0xFF888888);
            graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + 1, 0xFFAAAAAA);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFFF2F3E5, false);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000_000L) return (count / 1_000_000_000L) + "B";
        if (count >= 1_000_000L) return (count / 1_000_000L) + "M";
        if (count >= 1_000L) return (count / 1_000L) + "k";
        return String.valueOf(count);
    }

    private boolean isMouseOverSearchBox(double mouseX, double mouseY) {
        return mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight();
    }

}