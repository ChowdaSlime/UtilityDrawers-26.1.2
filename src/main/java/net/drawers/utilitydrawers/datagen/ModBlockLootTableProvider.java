package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.TEST_BLOCK.get());
        dropSelf(ModBlocks.DRAWER_BASE.get());
        dropSelf(ModBlocks.STORAGE_INTERFACE.get());
        dropSelf(ModBlocks.DRAWER_FRAMER.get());
        dropSelf(ModBlocks.STORAGE_VIEWER.get());

        for (Block drawer : ModBlocks.getAllDrawerBlocks()) {
            this.add(drawer, createDrawerDrop(drawer));
        }
        for (Block fluidDrawer : ModBlocks.getAllFluidDrawerBlocks()) {
            this.add(fluidDrawer, createDrawerDrop(fluidDrawer));
        }
        this.add(ModBlocks.COMPACTING_DRAWER.get(), createDrawerDrop(ModBlocks.COMPACTING_DRAWER.get()));
        for (Block framedDrawer : ModBlocks.getAllFramedDrawerBlocks()) {
            this.add(framedDrawer, createDrawerDrop(framedDrawer));
        }
        for (Block framedFluidDrawer : ModBlocks.getAllFramedFluidDrawerBlocks()) {
            this.add(framedFluidDrawer, createDrawerDrop(framedFluidDrawer));
        }
        this.add(ModBlocks.FRAMED_COMPACTING_DRAWER.get(), createDrawerDrop(ModBlocks.FRAMED_COMPACTING_DRAWER.get()));
        for (Block wirelessDrawer : ModBlocks.getAllWirelessDrawerBlocks()) {
            this.add(wirelessDrawer, createDrawerDrop(wirelessDrawer));
        }
        for (Block wirelessFluidDrawer : ModBlocks.getAllWirelessFluidDrawerBlocks()) {
            this.add(wirelessFluidDrawer, createDrawerDrop(wirelessFluidDrawer));
        }
    }

    protected LootTable.Builder createDrawerDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(DataComponents.CUSTOM_DATA))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
