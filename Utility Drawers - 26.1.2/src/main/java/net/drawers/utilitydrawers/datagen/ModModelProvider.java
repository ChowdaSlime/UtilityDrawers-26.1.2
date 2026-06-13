package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, UtilityDrawers.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.TEST_ITEM.get(), ModelTemplates.FLAT_ITEM);

        blockModels.createTrivialCube(ModBlocks.TEST_BLOCK.get());

    }
}
