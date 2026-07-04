package net.drawers.utilitydrawers.recipe;

import net.drawers.utilitydrawers.item.ExtractUpgradeItem;
import net.drawers.utilitydrawers.item.InsertUpgradeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ResetUpgradeRecipe extends CustomRecipe {

    public ResetUpgradeRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int upgradeCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isResettableUpgrade(stack)) {
                upgradeCount++;
            } else {
                return false;
            }
        }

        return upgradeCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && isResettableUpgrade(stack)) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    private static boolean isResettableUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ExtractUpgradeItem
                || stack.getItem() instanceof InsertUpgradeItem;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipeSerializers.RESET_UPGRADE.get();
    }
}