package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.UtilityDrawersConfig;
import net.chowdaslime.utilitydrawers.block.entity.FilingCabinetBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FilingCabinetMenu extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int CELL_SIZE = 18;
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 18;
    private static final int OFFSCREEN = -1000;
    public static final int VISIBLE_ROWS = 6;
    public static final int VIEWPORT_HEIGHT = VISIBLE_ROWS * CELL_SIZE;
    private int scrollOffset = 0;

    private final FilingCabinetBlockEntity blockEntity;
    private final Player player;
    private String searchText = "";

    public int getContentHeightSafe() {
        return Math.max(1, contentHeight);
    }

    public record HeaderEntry(FilingCabinetBlockEntity.Category category, int y) {}
    private final List<HeaderEntry> headers = new ArrayList<>();
    private int contentHeight = 0;

    public FilingCabinetMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public FilingCabinetMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.FILING_CABINET_MENU.get(), containerId);
        this.blockEntity = (FilingCabinetBlockEntity) blockEntity;
        this.player = playerInventory.player;

        for (int i = 0; i < UtilityDrawersConfig.FILING_CABINET_CAPACITY.get(); i++) {
            this.addSlot(new Slot(this.blockEntity, i, OFFSCREEN, OFFSCREEN));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }

        if (!this.blockEntity.getLevel().isClientSide()) {
            this.blockEntity.startOpen(player);
        }

        rebuildLayout();
    }

    public FilingCabinetBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public String getSearchText() {
        return searchText;
    }

    public List<HeaderEntry> getHeaders() {
        return headers;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public void setSearchText(String text) {
        this.searchText = text == null ? "" : text;
        rebuildLayout();
    }

    private void repositionSlot(int rawIndex, int x, int y) {
        Slot oldSlot = this.slots.get(rawIndex);
        Slot newSlot = new Slot(this.blockEntity, rawIndex, x, y);
        newSlot.index = oldSlot.index;

        this.slots.set(rawIndex, newSlot);
    }

    public int getScrollOffset() {
        return scrollOffset; }

    public int getMaxScrollOffset() {
        return Math.max(0, contentHeight - VIEWPORT_HEIGHT);
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(getMaxScrollOffset(), offset));
        rebuildLayout();
    }

    private void rebuildLayout() {
        headers.clear();

        List<Integer> withItems = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();

        int capacity = blockEntity.getContainerSize();
        for (int i = 0; i < capacity; i++) {
            ItemStack stack = blockEntity.getItem(i);
            if (stack.isEmpty()) {
                empty.add(i);
            } else if (matchesSearch(stack)) {
                withItems.add(i);
            }
        }

        withItems.sort(Comparator
                .comparing((Integer i) -> FilingCabinetBlockEntity.Category.of(blockEntity.getItem(i)))
                .thenComparing(i -> blockEntity.getItem(i).getHoverName().getString()));

        for (int i = 0; i < capacity; i++) {
            repositionSlot(i, OFFSCREEN, OFFSCREEN);
        }

        int logicalY = 0;
        int col = 0;
        FilingCabinetBlockEntity.Category currentCategory = null;

        for (int rawIndex : withItems) {
            FilingCabinetBlockEntity.Category cat = FilingCabinetBlockEntity.Category.of(blockEntity.getItem(rawIndex));
            if (cat != currentCategory) {
                if (currentCategory != null) {
                    if (col != 0) { logicalY += CELL_SIZE; col = 0; }
                }
                headers.add(new HeaderEntry(cat, logicalY));
                logicalY += 12;
                currentCategory = cat;
            }

            placeAt(rawIndex, col, logicalY);

            col++;
            if (col >= COLS) {
                col = 0;
                logicalY += CELL_SIZE;
            }
        }
        if (col != 0) logicalY += CELL_SIZE;

        if (searchText.isEmpty() && !empty.isEmpty()) {
            logicalY += 4;
            for (int rawIndex : empty) {
                placeAt(rawIndex, col, logicalY);
                col++;
                if (col >= COLS) {
                    col = 0;
                    logicalY += CELL_SIZE;
                }
            }
            if (col != 0) logicalY += CELL_SIZE;
        }

        contentHeight = logicalY;

        if (scrollOffset > getMaxScrollOffset()) {
            scrollOffset = getMaxScrollOffset();
        }
    }

    private void placeAt(int rawIndex, int col, int logicalY) {
        int screenY = GRID_TOP + logicalY - scrollOffset;
        if (screenY < GRID_TOP || screenY + CELL_SIZE > GRID_TOP + VIEWPORT_HEIGHT) {
            repositionSlot(rawIndex, OFFSCREEN, OFFSCREEN);
        } else {
            repositionSlot(rawIndex, GRID_LEFT + col * CELL_SIZE, screenY);
        }
    }

    private boolean matchesSearch(ItemStack stack) {
        if (searchText == null || searchText.isEmpty()) return true;
        String query = searchText.toLowerCase().trim();

        if (searchText.startsWith("@")) {
            String modFilter = query.substring(1).trim();
            if (modFilter.isEmpty()) return true;
            String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
            return namespace.toLowerCase().contains(modFilter);
        }

        if (searchText.startsWith("#")) {
            String tagFilter = query.substring(1).trim();
            if (tagFilter.isEmpty()) return true;

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) return false;
            return BuiltInRegistries.ITEM.get(itemId)
                    .map(holder -> holder.tags().anyMatch(tag ->
                            tag.location().getPath().toLowerCase().contains(tagFilter) ||
                                    tag.location().toString().toLowerCase().contains(tagFilter)))
                    .orElse(false);
        }

        if (searchText.startsWith("$")) {
            String tipFilter = query.substring(1).trim();
            if (tipFilter.isEmpty()) return true;

            List<Component> lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.Default.ADVANCED);
            return lines.stream()
                    .map(c -> c.getString().toLowerCase())
                    .anyMatch(line -> line.contains(tipFilter));
        }

        return stack.getHoverName().getString().toLowerCase().contains(query);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) < 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!blockEntity.getLevel().isClientSide()) {
            blockEntity.stopOpen(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            original = stackInSlot.copy();

            int cabinetCount = UtilityDrawersConfig.FILING_CABINET_CAPACITY.get();
            int invStart = cabinetCount;
            int hotbarStart = cabinetCount + 27;
            int invEnd = cabinetCount + 36;

            if (index < cabinetCount) {
                if (!this.moveItemStackTo(stackInSlot, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stackInSlot, 0, cabinetCount, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }

        rebuildLayout();
        return original;
    }
}