package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StorageViewerScreen extends AbstractContainerScreen<StorageViewerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/storage_viewer_gui.png");

    private static final int GUI_WIDTH = 195;

    private static final int GRID_COLS = 9;
    private static final int GRID_CELL_SIZE = 18;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 17;

    private static final int SCROLLBAR_X = 178;
    private static final int SCROLLBAR_Y = 18;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int THUMB_HEIGHT = 15;

    private static final int SEARCH_X = 41;
    private static final int SEARCH_Y = 3;
    private static final int SEARCH_WIDTH = 102;
    private static final int SEARCH_HEIGHT = 12;

    private static final int SORT_BUTTON_X = -20;
    private static final int SORT_BUTTON_Y = 9;

    private static final int SORT_DIR_BUTTON_X = -20;
    private static final int SORT_DIR_BUTTON_Y = 29;

    private static final int SIZE_BUTTON_X = -20;
    private static final int SIZE_BUTTON_Y = 49;

    private static final int JEI_BUTTON_Y = 69;

    private static final int TEX_SORT_COUNT_U = 14;
    private static final int TEX_SORT_NAME_U = 37;
    private static final int TEX_SORT_ASC_U = 60;
    private static final int TEX_SORT_DESC_U = 83;
    private static final int TEX_SORT_V = 174;
    private static final int TEX_SORT_SIZE = 20;
    private static final int TEX_BUTTON_JEI_U    = 175;
    private static final int TEX_BUTTON_SIZE_1_U = 106;
    private static final int TEX_BUTTON_SIZE_2_U = 129;
    private static final int TEX_BUTTON_SIZE_3_U = 152;
    private static final int TEX_BUTTON_V = 174;
    private static final int BUTTON_SIZE = 20;

    private EditBox searchBox;
    protected int scrollOffset = 0;
    private boolean isDraggingScroll = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;
    private int hoveredCell = -1;
    private boolean wasShiftDown = false;

    public List<StorageViewerMenu.NetworkSlot> filteredSlots = new ArrayList<>();

    public StorageViewerScreen(StorageViewerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public int getImageHeight() {
        return 171 + (menu.viewerRows - 3) * 18;
    }

    private int getScrollbarHeight() {
        return 52 + (menu.viewerRows - 3) * 18;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - getImageHeight()) / 2;

        searchBox = new EditBox(this.font,
                this.leftPos + SEARCH_X, this.topPos + SEARCH_Y,
                SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.literal("Search..."));
        searchBox.setMaxLength(64);
        searchBox.setBordered(false);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFF2F3E5);
        searchBox.setHint(Component.literal("Search...")
                .withStyle(s -> s.withColor(0xFFF2F3E5)));
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            rebuildFilteredSlots();
            if (menu.syncJei) {
                net.chowdaslime.utilitydrawers.jei.JeiWrapper.setFilterText(text);
            }
        });
        this.addRenderableWidget(this.searchBox);

        rebuildFilteredSlots();
    }

    public void rebuildFilteredSlots() {
        String raw = searchBox == null ? "" : searchBox.getValue().trim();
        List<StorageViewerMenu.NetworkSlot> all = menu.getNetworkSlots();

        Comparator<StorageViewerMenu.NetworkSlot> comparator = menu.sortByCount
                ? Comparator.comparingLong(ns -> ns.isFluid() ? ns.count() / 1000 : ns.count())
                : Comparator.comparing(ns -> ns.stack().getHoverName().getString().toLowerCase());

        if (!menu.sortAscending) {
            comparator = comparator.reversed();
        }

        filteredSlots = all.stream()
                .filter(ns -> matchesQuery(ns.stack(), raw))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(ItemStack stack, String raw) {
        if (raw.isEmpty()) return true;
        String query = raw.toLowerCase();

        if (raw.startsWith("@")) {
            String modFilter = query.substring(1).trim();
            if (modFilter.isEmpty()) return true;
            String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
            return namespace.toLowerCase().contains(modFilter);
        }

        if (raw.startsWith("#")) {
            String tagFilter = query.substring(1).trim();
            if (tagFilter.isEmpty()) return true;

            Fluid fluid = getFluidFromItem(stack);
            if (fluid != Fluids.EMPTY) {
                Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid);
                if (fluidId != null) {
                    Optional<? extends Holder<Fluid>> fluidHolder = BuiltInRegistries.FLUID.get(fluidId);
                    if (fluidHolder.isPresent()) {
                        boolean fluidMatch = fluidHolder.get().tags().anyMatch(tag ->
                                tag.location().getPath().toLowerCase().contains(tagFilter) ||
                                        tag.location().toString().toLowerCase().contains(tagFilter));
                        if (fluidMatch) return true;
                    }
                }
            }

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) return false;
            return BuiltInRegistries.ITEM.get(itemId)
                    .map(holder -> holder.tags().anyMatch(tag ->
                            tag.location().getPath().toLowerCase().contains(tagFilter) ||
                                    tag.location().toString().toLowerCase().contains(tagFilter)))
                    .orElse(false);
        }

        if (raw.startsWith("$")) {
            String tipFilter = query.substring(1).trim();
            if (tipFilter.isEmpty()) return true;
            Minecraft mc = Minecraft.getInstance();
            Item.TooltipContext ctx = Item.TooltipContext.of(mc.level);
            List<Component> lines = stack.getTooltipLines(ctx, mc.player, TooltipFlag.Default.ADVANCED);
            return lines.stream()
                    .map(c -> c.getString().toLowerCase())
                    .anyMatch(line -> line.contains(tipFilter));
        }

        return stack.getHoverName().getString().toLowerCase().contains(query);
    }

    private void updateFrozenSlots() {
        List<StorageViewerMenu.NetworkSlot> all = menu.getNetworkSlots();
        for (int i = 0; i < filteredSlots.size(); i++) {
            StorageViewerMenu.NetworkSlot oldSlot = filteredSlots.get(i);
            StorageViewerMenu.NetworkSlot newSlot = all.stream()
                    .filter(ns -> ItemStack.isSameItemSameComponents(ns.stack(), oldSlot.stack()) && ns.isFluid() == oldSlot.isFluid())
                    .findFirst()
                    .orElse(new StorageViewerMenu.NetworkSlot(oldSlot.stack(), 0, oldSlot.sources(), oldSlot.isFluid()));
            filteredSlots.set(i, newSlot);
        }
    }

    private int totalRows() {
        return (int) Math.ceil((double) filteredSlots.size() / GRID_COLS);
    }

    protected int maxScrollOffset() {
        return Math.max(0, totalRows() - menu.viewerRows);
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

        int btnX = this.leftPos + SIZE_BUTTON_X;
        int jeiY = this.topPos + JEI_BUTTON_Y;

        if (mouseX >= btnX && mouseX <= btnX + BUTTON_SIZE && mouseY >= jeiY && mouseY <= jeiY + BUTTON_SIZE) {
            menu.syncJei = !menu.syncJei;
            ClientPacketDistributor.sendToServer(new ToggleJeiSyncPayload(menu.syncJei));
            return true;
        }

        if (isMouseOverSearchBox(mouseX, mouseY)) {
            searchBox.setFocused(true);
            return true;
        }
        if (!isMouseOverSearchBox(mouseX, mouseY)) {
            searchBox.setFocused(false);
        }

        int sizeX = this.leftPos + SIZE_BUTTON_X;
        int sizeY = this.topPos + SIZE_BUTTON_Y;
        if (mouseX >= sizeX && mouseX <= sizeX + BUTTON_SIZE && mouseY >= sizeY && mouseY <= sizeY + BUTTON_SIZE) {
            int newRows = menu.viewerRows == 3 ? 6 : menu.viewerRows == 6 ? 9 : 3;
            changeViewerSize(newRows);
            return true;
        }

        int sortX = this.leftPos + SORT_BUTTON_X;
        int sortY = this.topPos + SORT_BUTTON_Y;
        if (mouseX >= sortX && mouseX <= sortX + TEX_SORT_SIZE && mouseY >= sortY && mouseY <= sortY + TEX_SORT_SIZE) {
            menu.sortByCount = !menu.sortByCount;
            rebuildFilteredSlots();
            ClientPacketDistributor.sendToServer(new ToggleSortPayload(menu.sortByCount, menu.sortAscending));
            return true;
        }

        int dirX = this.leftPos + SORT_DIR_BUTTON_X;
        int dirY = this.topPos + SORT_DIR_BUTTON_Y;
        if (mouseX >= dirX && mouseX <= dirX + TEX_SORT_SIZE && mouseY >= dirY && mouseY <= dirY + TEX_SORT_SIZE) {
            menu.sortAscending = !menu.sortAscending;
            rebuildFilteredSlots();
            ClientPacketDistributor.sendToServer(new ToggleSortPayload(menu.sortByCount, menu.sortAscending));
            return true;
        }

        int sbX = this.leftPos + SCROLLBAR_X;
        int thumbY = getThumbY();
        if (mouseX >= sbX && mouseX <= sbX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY <= thumbY + THUMB_HEIGHT) {
            isDraggingScroll = true;
            dragStartY = (int) mouseY;
            dragStartOffset = scrollOffset;
            return true;
        }

        int cellIndex = getCellAt((int) mouseX, (int) mouseY);

        if (cellIndex != -1) {
            boolean clickedValidSlot = cellIndex < filteredSlots.size();
            StorageViewerMenu.NetworkSlot slot = clickedValidSlot ? filteredSlots.get(cellIndex) : null;

            ItemStack cursor = menu.getCarried();
            boolean shiftHeld = minecraft.hasShiftDown();
            boolean cursorIsEmptyBucket = !cursor.isEmpty() && cursor.is(Items.BUCKET);

            boolean cursorIsFilledBucket = false;
            if (!cursor.isEmpty() && !cursor.is(Items.BUCKET)) {
                ResourceHandler<?> fluidHandler = cursor.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(cursor));
                if (fluidHandler != null) {
                    for (int i = 0; i < fluidHandler.size(); i++) {
                        if (fluidHandler.getAmountAsLong(i) > 0) {
                            cursorIsFilledBucket = true;
                            break;
                        }
                    }
                }
            }

            boolean slotIsFluid = clickedValidSlot && slot.isFluid();

            if (clickedValidSlot && slot.count() <= 0 && cursor.isEmpty()) {
                return true;
            }

            if (cursor.isEmpty()) {
                if (clickedValidSlot) {
                    if (shiftHeld && button == 0) {
                        ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                                slot.stack().copyWithCount(1),
                                slot.stack().getMaxStackSize(),
                                slot.sources(),
                                StorageViewerExtractPayload.ExtractMode.SHIFT_MOVE
                        ));
                    } else if (shiftHeld && button == 1) {
                        ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                                slot.stack().copyWithCount(1),
                                1,
                                slot.sources(),
                                StorageViewerExtractPayload.ExtractMode.CURSOR_FULL
                        ));
                    } else if (button == 0) {
                        int amount = slotIsFluid ? 1 : slot.stack().getMaxStackSize();
                        ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                                slot.stack().copyWithCount(1),
                                amount,
                                slot.sources(),
                                StorageViewerExtractPayload.ExtractMode.CURSOR_FULL
                        ));
                    } else if (button == 1) {
                        ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                                slot.stack().copyWithCount(1),
                                (int) Math.min(slot.count(), slot.stack().getMaxStackSize()),
                                slot.sources(),
                                StorageViewerExtractPayload.ExtractMode.CURSOR_HALF
                        ));
                    }
                }
            } else if (cursorIsEmptyBucket && slotIsFluid && button == 0 && clickedValidSlot) {
                ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                        slot.stack().copyWithCount(1),
                        1,
                        slot.sources(),
                        StorageViewerExtractPayload.ExtractMode.CURSOR_FULL
                ));
            } else if (cursorIsFilledBucket && button == 1) {
                ClientPacketDistributor.sendToServer(
                        new StorageViewerInsertPayload(cursor.copyWithCount(1), 1, true)
                );
            } else if (!cursor.isEmpty()) {
                if (shiftHeld) {
                    if (clickedValidSlot && button == 1 && !slotIsFluid
                            && ItemStack.isSameItemSameComponents(cursor, slot.stack())
                            && cursor.getCount() < cursor.getMaxStackSize()
                            && slot.count() > 0) {
                        ClientPacketDistributor.sendToServer(new StorageViewerExtractPayload(
                                slot.stack().copyWithCount(1),
                                1,
                                slot.sources(),
                                StorageViewerExtractPayload.ExtractMode.CURSOR_FULL
                        ));
                    }
                } else {
                    int insertAmount = (button == 0) ? cursor.getCount() : 1;
                    ClientPacketDistributor.sendToServer(
                            new StorageViewerInsertPayload(cursor.copyWithCount(1), insertAmount, false)
                    );
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void changeViewerSize(int targetRows) {
        if (menu.viewerRows != targetRows) {
            menu.updateSlotPositions(targetRows);
            ClientPacketDistributor.sendToServer(new CycleViewerSizePayload(targetRows));
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isDraggingScroll && maxScrollOffset() > 0) {
            int delta = (int) event.y() - dragStartY;
            int trackHeight = getScrollbarHeight() - THUMB_HEIGHT;
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

    protected int getThumbY() {
        int max = maxScrollOffset();
        int trackHeight = getScrollbarHeight() - THUMB_HEIGHT;
        int thumbOffset = max > 0 ? (int) Math.round((double) scrollOffset / max * trackHeight) : 0;
        return this.topPos + SCROLLBAR_Y + thumbOffset;
    }

    public int getCellAt(int mouseX, int mouseY) {
        int gx = this.leftPos + GRID_LEFT;
        int gy = this.topPos + GRID_TOP;
        if (mouseX < gx || mouseX >= gx + GRID_COLS * GRID_CELL_SIZE) return -1;
        if (mouseY < gy || mouseY >= gy + menu.viewerRows * GRID_CELL_SIZE) return -1;
        int col = (mouseX - gx) / GRID_CELL_SIZE;
        int row = (mouseY - gy) / GRID_CELL_SIZE;
        return (scrollOffset + row) * GRID_COLS + col;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int bx = this.leftPos;
        int by = this.topPos;
        int topHeight = 17;
        int bottomV = 71;
        int bottomHeight = 99;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, by, 0, 0, GUI_WIDTH, topHeight, 256, 256);

        for (int r = 0; r < menu.viewerRows; r++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, by + topHeight + (r * 18), 0, 17, GUI_WIDTH, 18, 256, 256);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, by + topHeight + (menu.viewerRows * 18), 0, bottomV, GUI_WIDTH, bottomHeight, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean isShiftDown = minecraft.hasShiftDown();
        if (this.wasShiftDown && !isShiftDown) {
            this.rebuildFilteredSlots();
        }
        if (this.menu.clientNeedsRebuild) {
            if (isShiftDown) {
                this.updateFrozenSlots();
            } else {
                this.rebuildFilteredSlots();
            }
            this.menu.clientNeedsRebuild = false;
        }
        this.wasShiftDown = isShiftDown;

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredCell >= 0) {
            boolean clickedValidSlot = hoveredCell < filteredSlots.size();
            StorageViewerMenu.NetworkSlot ns = clickedValidSlot ? filteredSlots.get(hoveredCell) : null;

            ItemStack cursor = menu.getCarried();
            boolean cursorIsEmptyBucket = !cursor.isEmpty() && cursor.is(Items.BUCKET);
            boolean cursorIsFilledBucket = false;
            if (!cursor.isEmpty() && !cursor.is(Items.BUCKET)) {
                ResourceHandler<?> fluidHandler = cursor.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(cursor));
                if (fluidHandler != null) {
                    for (int i = 0; i < fluidHandler.size(); i++) {
                        if (fluidHandler.getAmountAsLong(i) > 0) {
                            cursorIsFilledBucket = true;
                            break;
                        }
                    }
                }
            }

            String slotDisplayName = "";
            if (clickedValidSlot) {
                if (ns.isFluid()) {
                    Fluid fluid = getFluidFromItem(ns.stack());
                    slotDisplayName = fluid != Fluids.EMPTY
                            ? Component.translatable(fluid.getFluidType().getDescriptionId()).getString()
                            : ns.stack().getHoverName().getString();
                } else {
                    slotDisplayName = ns.stack().getHoverName().getString();
                }
            }

            List<Component> tooltip = new ArrayList<>();
            boolean shiftDown = minecraft.hasShiftDown();
            boolean advancedTooltips = minecraft.options.advancedItemTooltips;

            if (clickedValidSlot && !ns.isFluid()) {
                Minecraft mc = Minecraft.getInstance();
                Item.TooltipContext ctx = Item.TooltipContext.of(mc.level);
                tooltip.addAll(ns.stack().getTooltipLines(ctx, mc.player, TooltipFlag.Default.NORMAL));

                if (shiftDown && advancedTooltips) {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(ns.stack().getItem());
                    if (itemId != null) {
                        BuiltInRegistries.ITEM.get(itemId).ifPresent(holder ->
                                holder.tags().forEach(tag ->
                                        tooltip.add(Component.literal("#" + tag.location())
                                                .withStyle(s -> s.withColor(0xFF888888)))
                                )
                        );
                    }
                }
            } else if (clickedValidSlot && ns.isFluid()) {
                tooltip.add(Component.literal(slotDisplayName));

                if (shiftDown && advancedTooltips) {
                    Fluid fluid = getFluidFromItem(ns.stack());
                    if (fluid != Fluids.EMPTY) {
                        Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid);
                        if (fluidId != null) {
                            BuiltInRegistries.FLUID.get(fluidId).ifPresent(holder ->
                                    holder.tags().forEach(tag ->
                                            tooltip.add(Component.literal("#" + tag.location())
                                                    .withStyle(s -> s.withColor(0xFF888888)))
                                    )
                            );
                        }
                    }
                }
            }
            if (clickedValidSlot) {
                if (ns.isFluid() && !cursorIsEmptyBucket && !cursorIsFilledBucket) {
                    tooltip.add(Component.literal("Left - click: Extract 1B of " + slotDisplayName + " From Network")
                            .withStyle(s -> s.withColor(0xFF888888)));
                    tooltip.add(Component.literal("(Auto-pulls buckets from Network or Inventory)")
                            .withStyle(s -> s.withColor(0xFF555555)));
                } else if (!ns.isFluid() && cursor.isEmpty()) {
                    tooltip.add(Component.literal("Left - click: Extract Stack")
                            .withStyle(s -> s.withColor(0xFF888888)));
                    tooltip.add(Component.literal("Right - click: Extract Half Stack")
                            .withStyle(s -> s.withColor(0xFF888888)));
                    tooltip.add(Component.literal("Shift + left - click: Quick Extract Stack")
                            .withStyle(s -> s.withColor(0xFF888888)));
                    tooltip.add(Component.literal("Shift + right - click: Extract 1 to Cursor")
                            .withStyle(s -> s.withColor(0xFF888888)));
                } else if (!ns.isFluid() && !cursor.isEmpty() && ItemStack.isSameItemSameComponents(cursor, ns.stack()) && cursor.getCount() < cursor.getMaxStackSize()) {
                    tooltip.add(Component.literal("Shift + right - click: Extract 1 to Cursor")
                            .withStyle(s -> s.withColor(0xFF888888)));
                }
            }

            if (cursorIsEmptyBucket) {
                if (clickedValidSlot && ns.isFluid()) {
                    tooltip.add(Component.literal("Left - click: Fill With " + slotDisplayName)
                            .withStyle(s -> s.withColor(0xFF888888)));
                } else {
                    tooltip.add(Component.literal("Left - click: Insert")
                            .withStyle(s -> s.withColor(0xFF888888)));
                    tooltip.add(Component.literal("Right - click: Insert 1")
                            .withStyle(s -> s.withColor(0xFF888888)));
                }
            } else if (cursorIsFilledBucket) {
                tooltip.add(Component.literal("Left - click: Insert")
                        .withStyle(s -> s.withColor(0xFF888888)));
                Fluid cursorFluid = getFluidFromItem(cursor);
                String cursorName = cursorFluid != Fluids.EMPTY
                        ? Component.translatable(cursorFluid.getFluidType().getDescriptionId()).getString()
                        : cursor.getHoverName().getString();
                tooltip.add(Component.literal("Right - click: Empty " + cursorName + " Into Network")
                        .withStyle(s -> s.withColor(0xFF888888)));
            } else if (!cursor.isEmpty() && !cursorIsEmptyBucket) {
                tooltip.add(Component.literal("Left - click/Right - click: Insert All/Insert")
                        .withStyle(s -> s.withColor(0xFF888888)));
            }

            if (!tooltip.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
            }
        }
        if (isMouseOverSearchBox(mouseX, mouseY)) {
            List<Component> searchTooltip = new ArrayList<>();
            searchTooltip.add(Component.literal("Search").withStyle(s -> s.withBold(true)));
            searchTooltip.add(Component.literal("@ - Search by mod").withStyle(s -> s.withColor(0xFF888888)));
            searchTooltip.add(Component.literal("# - Search by tag").withStyle(s -> s.withColor(0xFF888888)));
            searchTooltip.add(Component.literal("$ - Search by tooltip").withStyle(s -> s.withColor(0xFF888888)));
            graphics.setTooltipForNextFrame(this.font, searchTooltip, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        int bx = this.leftPos;
        int by = this.topPos;

        int sortX = bx + SORT_BUTTON_X;
        int sortY = by + SORT_BUTTON_Y;
        int sortTypeU = menu.sortByCount ? TEX_SORT_COUNT_U : TEX_SORT_NAME_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                sortX, sortY, sortTypeU, TEX_SORT_V,
                TEX_SORT_SIZE, TEX_SORT_SIZE, 256, 256);

        int dirX = bx + SORT_DIR_BUTTON_X;
        int dirY = by + SORT_DIR_BUTTON_Y;
        int sortDirU = menu.sortAscending ? TEX_SORT_ASC_U : TEX_SORT_DESC_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                dirX, dirY, sortDirU, TEX_SORT_V,
                TEX_SORT_SIZE, TEX_SORT_SIZE, 256, 256);

        int sizeX = bx + SIZE_BUTTON_X;
        int sizeY = by + SIZE_BUTTON_Y;
        int sizeU = menu.viewerRows == 3 ? TEX_BUTTON_SIZE_1_U : menu.viewerRows == 6 ? TEX_BUTTON_SIZE_2_U : TEX_BUTTON_SIZE_3_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                sizeX, sizeY, sizeU, TEX_BUTTON_V,
                BUTTON_SIZE, BUTTON_SIZE, 256, 256);

        int jeiY = by + JEI_BUTTON_Y;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                sizeX, jeiY, TEX_BUTTON_JEI_U, TEX_BUTTON_V,
                BUTTON_SIZE, BUTTON_SIZE, 256, 256);

        if (menu.syncJei) {
            graphics.outline(sizeX, jeiY, BUTTON_SIZE, BUTTON_SIZE, 0xFFE0E0E0);
        }

        int gx = bx + GRID_LEFT;
        int gy = by + GRID_TOP;
        hoveredCell = getCellAt(mouseX, mouseY);

        for (int row = 0; row < menu.viewerRows; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int cellIndex = (scrollOffset + row) * GRID_COLS + col;
                int cx = gx + col * GRID_CELL_SIZE;
                int cy = gy + row * GRID_CELL_SIZE;

                if (cellIndex < filteredSlots.size()) {
                    StorageViewerMenu.NetworkSlot ns = filteredSlots.get(cellIndex);
                    boolean isGhost = ns.count() <= 0;

                    if (ns.isFluid()) {
                        Fluid fluid = getFluidFromItem(ns.stack());
                        if (fluid != Fluids.EMPTY) {
                            FluidStack fStack = new FluidStack(fluid, 1000);
                            drawFluid(graphics, fStack, cx, cy, 16, 16);
                        } else {
                            graphics.item(ns.stack().copyWithCount(1), cx + 1, cy + 1);
                        }
                    } else {
                        graphics.item(ns.stack().copyWithCount(1), cx + 1, cy + 1);
                    }

                    if (isGhost) {
                        graphics.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x88000000);
                    }

                    if (!isGhost) {
                        String countStr = formatCount(ns.count(), ns.isFluid());
                        graphics.nextStratum();
                        graphics.pose().pushMatrix();
                        float textScale = 0.75f;
                        graphics.pose().translate(cx + 17, cy + 17);
                        graphics.pose().scale(textScale, textScale);
                        int textWidth = this.font.width(countStr);
                        graphics.text(this.font, Component.literal(countStr), -textWidth, -this.font.lineHeight, 0xFFFFFFFF, true);
                        graphics.pose().popMatrix();
                    }
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
        int dynamicInventoryY = 16 + (menu.viewerRows * 18) + 3;

        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, dynamicInventoryY + 2, 0xFFF2F3E5, false);
    }

    protected String formatCount(long count, boolean isFluid) {
        if (isFluid) {
            if (count == 0) return "0";
            if (count >= 1000) {
                if (count % 1000 == 0) {
                    return (count / 1000) + "B";
                }
                return String.format("%.1fB", count / 1000.0f);
            }
            return count + "mB";
        }
        if (count >= 1_000_000_000L) return (count / 1_000_000_000L) + "G";
        if (count >= 1_000_000L) return (count / 1_000_000L) + "M";
        if (count >= 1_000L) return (count / 1_000L) + "k";
        return String.valueOf(count);
    }

    private boolean isMouseOverSearchBox(double mouseX, double mouseY) {
        return mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight();
    }

    protected Fluid getFluidFromItem(ItemStack stack) {
        if (stack.getItem() instanceof BucketItem bucketItem) {
            return bucketItem.getContent();
        }
        return Fluids.EMPTY;
    }

    protected void drawFluid(GuiGraphicsExtractor graphics, FluidStack stack, int x, int y, int width, int height) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        TextureAtlasSprite sprite;
        try {
            var fluidModel = mc.getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState());
            sprite = fluidModel.stillMaterial().sprite();
        } catch (Exception e) { return; }
        int color;
        try {
            var fluidModel = mc.getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState());
            var tintSource = fluidModel.fluidTintSource();
            color = tintSource != null ? tintSource.colorAsStack(stack) : 0xFFFFFFFF;
        } catch (Exception e) { color = 0xFFFFFFFF; }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, color);
    }
}