package net.chowdaslime.utilitydrawers.menu;

import net.chowdaslime.utilitydrawers.block.ModBlocks;
import net.chowdaslime.utilitydrawers.block.entity.CraftingStorageViewerBlockEntity;
import net.chowdaslime.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.chowdaslime.utilitydrawers.network.SyncNetworkSlotsPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;

public class CraftingStorageViewerMenu extends StorageViewerMenu {
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final CraftingStorageViewerBlockEntity craftingBlockEntity;
    private boolean restoringGrid = false;

    public CraftingStorageViewerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                buf.readBoolean() ? (StorageInterfaceBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()) : null,
                buf.readBlockPos(),
                null);
    }

    public CraftingStorageViewerMenu(int containerId, Inventory playerInventory, StorageInterfaceBlockEntity storageInterface, BlockPos viewerPos, CraftingStorageViewerBlockEntity craftingBlockEntity) {
        super(ModMenuTypes.CRAFTING_STORAGE_VIEWER_MENU.get(), containerId, playerInventory, storageInterface, viewerPos);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), viewerPos);
        this.craftingBlockEntity = craftingBlockEntity;

        int yOffset = (this.viewerRows - 3) * 18;

        this.addSlot(new AutoRefillResultSlot(this, this.player, this.craftSlots, this.resultSlots, 0, 135, 111 + yOffset));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 26 + j * 18, 93 + i * 18 + yOffset));
            }
        }

        if (craftingBlockEntity != null) {
            restoringGrid = true;
            NonNullList<ItemStack> saved = craftingBlockEntity.getCraftGridContents();
            for (int i = 0; i < 9; i++) {
                this.craftSlots.setItem(i, saved.get(i).copy());
            }
            restoringGrid = false;
            this.access.execute((level, pos) -> slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots));
        }

        this.updateSlotPositions(this.viewerRows);
    }

    public void refillCraftGridFromNetwork(ItemStack[] before) {
        boolean changed = false;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack template = before[i];
            if (template.isEmpty()) continue;
            if (!craftSlots.getItem(i).isEmpty()) continue;

            ItemStack extracted = extractOneFromNetworkThenInventory(template.copyWithCount(1));
            if (!extracted.isEmpty()) {
                craftSlots.setItem(i, extracted);
                changed = true;
            }
        }

        this.access.execute((level, pos) -> slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots));
        saveCraftGridState();

        if (changed) {
            this.refreshNetworkSlots();
            if (this.player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new SyncNetworkSlotsPayload(this.networkSlots));
            }
        }
    }

    private ItemStack extractOneFromNetworkThenInventory(ItemStack target) {
        if (target.isEmpty()) return ItemStack.EMPTY;

        StorageInterfaceBlockEntity iface = getStorageInterface();
        if (iface != null) {
            ItemStack fromNetwork = iface.extractItemFromNetwork(target, 1);
            if (!fromNetwork.isEmpty()) {
                return fromNetwork;
            }
        }

        return extractFromPlayerInventory(target, 1);
    }

    private ItemStack extractFromPlayerInventory(ItemStack target, int amount) {
        Inventory inv = this.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack invStack = inv.getItem(i);
            if (ItemStack.isSameItemSameComponents(invStack, target)) {
                return inv.removeItem(i, amount);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == 36) {
            return craftFullStackFromResultSlot(player);
        }
        return super.quickMoveStack(player, index);
    }

    private ItemStack craftFullStackFromResultSlot(Player player) {
        Slot resultSlotObj = this.slots.get(36);
        if (!resultSlotObj.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack template = resultSlotObj.getItem().copy();
        int maxStack = template.getMaxStackSize();
        ItemStack accumulated = template.copyWithCount(0);

        while (resultSlotObj.hasItem()
                && ItemStack.isSameItemSameComponents(resultSlotObj.getItem(), template)
                && accumulated.getCount() < maxStack) {

            ItemStack crafted = resultSlotObj.getItem().copy();
            int spaceLeft = maxStack - accumulated.getCount();
            if (crafted.getCount() > spaceLeft) {
                break;
            }

            ItemStack takenStack = resultSlotObj.remove(crafted.getCount());
            resultSlotObj.onTake(player, takenStack);
            accumulated.grow(takenStack.getCount());
        }

        if (accumulated.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!player.getInventory().add(accumulated)) {
            player.drop(accumulated, false);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void updateSlotPositions(int newRows) {
        this.viewerRows = newRows;
        int yOffset = (newRows - 3) * 18; 

        for (int i = 0; i < 36; i++) {
            Slot oldSlot = this.slots.get(i);
            int containerIndex = oldSlot.getSlotIndex();
            int newX, newY;

            if (containerIndex < 9) {
                newX = 8 + containerIndex * 18;
                newY = 226 + yOffset;
            } else {
                int col = (containerIndex - 9) % 9;
                int row = (containerIndex - 9) / 9;
                newX = 8 + col * 18;
                newY = 167 + row * 18 + yOffset;
            }

            Slot newSlot = new Slot(oldSlot.container, containerIndex, newX, newY);
            newSlot.index = i;
            this.slots.set(i, newSlot);
        }

        if (this.slots.size() > 36) {
            Slot oldResult = this.slots.get(36);
            AutoRefillResultSlot newResultSlot = new AutoRefillResultSlot(this, this.player, this.craftSlots, this.resultSlots, 0, 135, 111 + yOffset);
            newResultSlot.index = 36;
            this.slots.set(36, newResultSlot);

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int index = 37 + (j + i * 3);
                    Slot oldCraft = this.slots.get(index);
                    Slot newCraftSlot = new Slot(this.craftSlots, j + i * 3, 26 + j * 18, 93 + i * 18 + yOffset);
                    newCraftSlot.index = index;
                    this.slots.set(index, newCraftSlot);
                }
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (restoringGrid) return;
        super.slotsChanged(container);
        this.access.execute((level, pos) -> slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots));
        saveCraftGridState();
        this.broadcastFullState();
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ItemStack itemstack = ItemStack.EMPTY;
            CraftingInput craftinginput = craftSlots.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftinginput, level);

            if (optional.isPresent()) {
                RecipeHolder<CraftingRecipe> recipeholder = optional.get();
                CraftingRecipe craftingrecipe = recipeholder.value();
                resultSlots.setRecipeUsed(recipeholder);

                ItemStack resultStack = craftingrecipe.assemble(craftinginput);
                if (resultStack.isItemEnabled(level.enabledFeatures())) {
                    itemstack = resultStack;
                }
            }

            resultSlots.setItem(0, itemstack);
            menu.setRemoteSlot(0, itemstack);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemstack));
        }
    }

    private void saveCraftGridState() {
        if (craftingBlockEntity == null) return;
        NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            contents.set(i, this.craftSlots.getItem(i).copy());
        }
        craftingBlockEntity.setCraftGridContents(contents);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveCraftGridState();
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (containerInput == ContainerInput.QUICK_MOVE && buttonNum == 0
                && slotIndex >= 37 && slotIndex <= 45) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                ItemStack remainder = insertIntoNetwork(stack, player);
                slot.set(remainder);
                slot.setChanged();
                this.slotsChanged(this.craftSlots);
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.CRAFTING_STORAGE_VIEWER.get());
    }

    public void handleJeiTransfer(Map<Integer, ItemStack> inputs, boolean maxTransfer) {
        for (int i = 0; i < this.craftSlots.getContainerSize(); i++) {
            this.craftSlots.setItem(i, ItemStack.EMPTY);
        }

        inputs.forEach((slotIndex, requiredStack) -> {
            if (slotIndex >= 0 && slotIndex < this.craftSlots.getContainerSize()) {
                ItemStack extracted = extractItemForJei(requiredStack, maxTransfer ? requiredStack.getMaxStackSize() : 1);
                this.craftSlots.setItem(slotIndex, extracted);
            }
        });

        this.slotsChanged(this.craftSlots);

        this.refreshNetworkSlots();
        if (this.player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkSlotsPayload(this.networkSlots));
        }
    }

    private ItemStack extractItemForJei(ItemStack target, int amount) {
        if (target.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = target.copyWithCount(0);
        int needed = amount;

        if (this.getStorageInterface() != null) {
            ItemStack pulled = this.getStorageInterface().extractItemFromNetwork(target, needed);
            if (!pulled.isEmpty()) {
                result.grow(pulled.getCount());
                needed -= pulled.getCount();
            }
        }

        if (needed <= 0) return result;

        for (int i = 0; i < this.player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = this.player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(invStack, target)) {
                int take = Math.min(needed, invStack.getCount());
                this.player.getInventory().removeItem(i, take);
                result.grow(take);
                needed -= take;
                if (needed <= 0) return result;
            }
        }

        return result.getCount() > 0 ? result : ItemStack.EMPTY;
    }
}