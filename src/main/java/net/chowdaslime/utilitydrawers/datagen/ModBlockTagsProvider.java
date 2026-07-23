package net.chowdaslime.utilitydrawers.datagen;

import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.block.ModBlocks;
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
            this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.getRK(drawer));
            this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.getRK(ModBlocks.DRAWER_FRAMER.get()));
        }
        for (Block fluidDrawer : ModBlocks.getAllFluidDrawerBlocks()) {
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(fluidDrawer));
            this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(fluidDrawer));
        }
        for (Block wirelessDrawer : ModBlocks.getAllWirelessDrawerBlocks()) {
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(wirelessDrawer));
            this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(wirelessDrawer));
        }
        for (Block wirelessFluidDrawer : ModBlocks.getAllWirelessFluidDrawerBlocks()) {
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(wirelessFluidDrawer));
            this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(wirelessFluidDrawer));
        }

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(ModBlocks.STORAGE_INTERFACE.get()));
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(ModBlocks.STORAGE_INTERFACE.get()));
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(ModBlocks.COMPACTING_DRAWER.get()));
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(ModBlocks.COMPACTING_DRAWER.get()));
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(ModBlocks.STORAGE_VIEWER.get()));
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(ModBlocks.STORAGE_VIEWER.get()));
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.getRK(ModBlocks.FILING_CABINET.get()));
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.getRK(ModBlocks.FILING_CABINET.get()));
    }
}

