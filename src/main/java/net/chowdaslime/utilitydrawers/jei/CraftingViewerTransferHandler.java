package net.chowdaslime.utilitydrawers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.chowdaslime.utilitydrawers.menu.CraftingStorageViewerMenu;
import net.chowdaslime.utilitydrawers.menu.StorageViewerMenu;
import net.chowdaslime.utilitydrawers.network.JeiRecipeTransferPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CraftingViewerTransferHandler implements IRecipeTransferHandler<CraftingStorageViewerMenu, RecipeHolder<CraftingRecipe>> {

    private final MenuType<CraftingStorageViewerMenu> menuType;
    private final IRecipeTransferHandlerHelper helper;

    public CraftingViewerTransferHandler(MenuType<CraftingStorageViewerMenu> menuType, IRecipeTransferHandlerHelper helper) {
        this.menuType = menuType;
        this.helper = helper;
    }

    @Override
    public Class<CraftingStorageViewerMenu> getContainerClass() {
        return CraftingStorageViewerMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingStorageViewerMenu>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public IRecipeHolderType<CraftingRecipe> getRecipeType() {
        return mezz.jei.api.constants.RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CraftingStorageViewerMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        Map<Integer, ItemStack> inputs = new HashMap<>();
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        int gridSlot = 0;

        Map<Item, Long> availableItems = new HashMap<>();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = container.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                availableItems.put(stack.getItem(), availableItems.getOrDefault(stack.getItem(), 0L) + stack.getCount());
            }
        }

        for (StorageViewerMenu.NetworkSlot ns : container.getNetworkSlots()) {
            if (!ns.isFluid() && !ns.stack().isEmpty()) {
                availableItems.put(ns.stack().getItem(), availableItems.getOrDefault(ns.stack().getItem(), 0L) + ns.count());
            }
        }

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews()) {
            if (slotView.getRole() == RecipeIngredientRole.INPUT) {
                boolean foundMatch = false;
                Optional<ItemStack> matchedStack = Optional.empty();
                List<ItemStack> possibleInputs = slotView.getIngredients(VanillaTypes.ITEM_STACK).toList();

                if (!possibleInputs.isEmpty()) {
                    for (ItemStack possible : possibleInputs) {
                        if (possible.isEmpty()) continue;
                        long available = availableItems.getOrDefault(possible.getItem(), 0L);
                        if (available > 0) {
                            availableItems.put(possible.getItem(), available - 1);
                            foundMatch = true;
                            matchedStack = Optional.of(possible);
                            break;
                        }
                    }

                    if (foundMatch) {
                        inputs.put(gridSlot, matchedStack.get().copyWithCount(1));
                    } else {
                        missingSlots.add(slotView);
                    }
                }
                gridSlot++;
            }
        }

        if (!missingSlots.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missingSlots);
        }

        if (doTransfer) {
            ClientPacketDistributor.sendToServer(new JeiRecipeTransferPayload(inputs, maxTransfer));
        }

        return null;
    }
}