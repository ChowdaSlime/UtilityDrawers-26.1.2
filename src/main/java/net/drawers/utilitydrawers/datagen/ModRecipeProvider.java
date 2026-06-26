package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

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

        shaped(RecipeCategory.MISC, ModBlocks.DRAWER_BASE.get())
                .pattern("ABA")
                .pattern("B B")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.UPGRADE_BASE.get())
                .pattern("ABA")
                .pattern("B B")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.DRAWER_UPGRADE_T1.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.IRON_BLOCK)
                .define('C', Items.CHEST)
                .define('D', ModItems.UPGRADE_BASE.get())
                .unlockedBy("has_upgrade_base", has(ModItems.UPGRADE_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.DRAWER_UPGRADE_T2.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.GOLD_INGOT)
                .define('B', Items.GOLD_BLOCK)
                .define('C', ModItems.DRAWER_UPGRADE_T1.get())
                .define('D', ModItems.UPGRADE_BASE.get())
                .unlockedBy("has_upgrade_t1", has(ModItems.DRAWER_UPGRADE_T1.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.DRAWER_UPGRADE_T3.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.DIAMOND)
                .define('B', Items.DIAMOND_BLOCK)
                .define('C', ModItems.DRAWER_UPGRADE_T2.get())
                .define('D', ModItems.UPGRADE_BASE.get())
                .unlockedBy("has_upgrade_t2", has(ModItems.DRAWER_UPGRADE_T2.get()))
                .save(output);

        SmithingTransformRecipeBuilder.smithing(
                        ingredient(ModItems.UPGRADE_BASE.get()),
                        ingredient(ModItems.DRAWER_UPGRADE_T3.get()),
                        ingredient(Items.NETHERITE_INGOT),
                        RecipeCategory.MISC,
                        ModItems.DRAWER_UPGRADE_T4.get()
                ).unlocks("has_upgrade_t3", has(ModItems.DRAWER_UPGRADE_T3.get()))
                .save(output, "utilitydrawers:drawer_upgrade_t4_smithing");

        shaped(RecipeCategory.MISC, ModItems.VOID_UPGRADE.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.OBSIDIAN)
                .define('B', Items.CRYING_OBSIDIAN)
                .define('C', ModItems.UPGRADE_BASE.get())
                .unlockedBy("has_crying_obsidian", has(Items.CRYING_OBSIDIAN))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.STORAGE_INTERFACE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.ENDER_PEARL)
                .define('C', Items.REDSTONE)
                .define('D', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.STORAGE_REMOTE.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.DIAMOND)
                .define('B', Items.IRON_INGOT)
                .define('C', Items.ENDER_PEARL)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        for (ModBlocks.WoodType wood : ModBlocks.WoodType.values()) {
            var strippedLog = getStrippedLog(wood);

            shaped(RecipeCategory.MISC, ModBlocks.getDrawer(wood, 1).get())
                    .pattern("AAA")
                    .pattern("ABA")
                    .pattern("AAA")
                    .define('A', strippedLog)
                    .define('B', ModBlocks.DRAWER_BASE.get())
                    .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                    .save(output);

            shaped(RecipeCategory.MISC, ModBlocks.getDrawer(wood, 2).get())
                    .pattern("AAA")
                    .pattern("CBC")
                    .pattern("AAA")
                    .define('A', strippedLog)
                    .define('B', ModBlocks.DRAWER_BASE.get())
                    .define('C', Items.CHEST)
                    .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                    .save(output);

            shaped(RecipeCategory.MISC, ModBlocks.getDrawer(wood, 3).get())
                    .pattern("ACA")
                    .pattern("ABA")
                    .pattern("CAC")
                    .define('A', strippedLog)
                    .define('B', ModBlocks.DRAWER_BASE.get())
                    .define('C', Items.CHEST)
                    .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                    .save(output);

            shaped(RecipeCategory.MISC, ModBlocks.getDrawer(wood, 4).get())
                    .pattern("CAC")
                    .pattern("ABA")
                    .pattern("CAC")
                    .define('A', strippedLog)
                    .define('B', ModBlocks.DRAWER_BASE.get())
                    .define('C', Items.CHEST)
                    .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                    .save(output);
        }

        shaped(RecipeCategory.MISC, ModBlocks.getFluidDrawer(1).get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.STONE)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFluidDrawer(2).get())
                .pattern("AAA")
                .pattern("CBC")
                .pattern("AAA")
                .define('A', Items.STONE)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFluidDrawer(3).get())
                .pattern("ACA")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STONE)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFluidDrawer(4).get())
                .pattern("CAC")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STONE)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.COMPACTING_DRAWER.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.IRON_BLOCK)
                .define('B', Items.PISTON)
                .define('C', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedDrawer(1).get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedDrawer(2).get())
                .pattern("AAA")
                .pattern("CBC")
                .pattern("AAA")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Blocks.CHEST)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedDrawer(3).get())
                .pattern("ACA")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Blocks.CHEST)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedDrawer(4).get())
                .pattern("CAC")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Blocks.CHEST)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedFluidDrawer(1).get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedFluidDrawer(2).get())
                .pattern("AAA")
                .pattern("CBC")
                .pattern("AAA")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedFluidDrawer(3).get())
                .pattern("ACA")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.getFramedFluidDrawer(4).get())
                .pattern("CAC")
                .pattern("ABA")
                .pattern("CAC")
                .define('A', Items.STICK)
                .define('B', ModBlocks.DRAWER_BASE.get())
                .define('C', Items.BUCKET)
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.FRAMED_COMPACTING_DRAWER.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.IRON_BLOCK)
                .define('B', Items.STICKY_PISTON)
                .define('C', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.DRAWER_FRAMER.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.STICK)
                .define('B', Items.IRON_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('D', ModBlocks.DRAWER_BASE.get())
                .unlockedBy("has_drawer_base", has(ModBlocks.DRAWER_BASE.get()))
                .save(output);
    }

    private net.minecraft.world.item.crafting.Ingredient ingredient(Item item) {
        return net.minecraft.world.item.crafting.Ingredient.of(item);
    }

    private Item getStrippedLog(ModBlocks.WoodType wood) {
        return switch (wood) {
            case OAK -> Items.STRIPPED_OAK_LOG;
            case SPRUCE -> Items.STRIPPED_SPRUCE_LOG;
            case BIRCH -> Items.STRIPPED_BIRCH_LOG;
            case ACACIA -> Items.STRIPPED_ACACIA_LOG;
            case JUNGLE -> Items.STRIPPED_JUNGLE_LOG;
            case DARK_OAK -> Items.STRIPPED_DARK_OAK_LOG;
            case MANGROVE -> Items.STRIPPED_MANGROVE_LOG;
            case CHERRY -> Items.STRIPPED_CHERRY_LOG;
            case PALE_OAK -> Items.STRIPPED_PALE_OAK_LOG;
            case BAMBOO -> Items.STRIPPED_BAMBOO_BLOCK;
            case CRIMSON -> Items.STRIPPED_CRIMSON_STEM;
            case WARPED -> Items.STRIPPED_WARPED_STEM;
        };
    }
}