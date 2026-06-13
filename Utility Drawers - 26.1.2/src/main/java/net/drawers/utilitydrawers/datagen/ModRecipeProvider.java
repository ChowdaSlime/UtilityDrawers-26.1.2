package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Utility Drawers Recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.TEST_ITEM.get())
                .pattern("BBB")
                .pattern("BAB")
                .pattern("BBB")
                .define('A', Items.WHITE_CONCRETE)
                .define('B', Items.GRAY_CONCRETE)
                .unlockedBy("has_white_concrete", has(Items.WHITE_CONCRETE))
                .save(output);

        shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.TEST_BLOCK.get())
                .requires(Items.WHITE_CONCRETE, 4)
                .unlockedBy("has_white_concrete", has(Items.WHITE_CONCRETE))
                .save(output);
    }
}
