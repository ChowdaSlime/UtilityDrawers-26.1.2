package net.drawers.utilitydrawers.datagen;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, UtilityDrawers.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // 1. Register test items/blocks
        itemModels.generateFlatItem(ModItems.TEST_ITEM.get(), ModelTemplates.FLAT_ITEM);
        blockModels.createTrivialCube(ModBlocks.TEST_BLOCK.get());

        for (Block drawer : ModBlocks.getAllDrawerBlocks()) {

            var modelLoc = ModelLocationUtils.getModelLocation(drawer);

            MultiVariant baseVariant = BlockModelGenerators.plainVariant(modelLoc);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(drawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(DrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
            );
        }
    }
}