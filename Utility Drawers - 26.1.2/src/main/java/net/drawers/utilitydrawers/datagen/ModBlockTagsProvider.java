package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, UtilityDrawers.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (Block drawer : ModBlocks.getAllDrawerBlocks()) {
            this.tag(BlockTags.MINEABLE_WITH_AXE).add(drawer);
        }
        for (Block fluidDrawer : ModBlocks.getAllFluidDrawerBlocks()) {
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(fluidDrawer);
        }

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STORAGE_INTERFACE.get());

        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.STORAGE_INTERFACE.get());
    }
}

