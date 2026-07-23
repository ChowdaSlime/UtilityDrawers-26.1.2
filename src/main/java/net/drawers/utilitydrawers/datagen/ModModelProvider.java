package net.drawers.utilitydrawers.datagen;

import com.mojang.math.Quadrant;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.*;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, UtilityDrawers.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.TEST_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAWER_UPGRADE_T1.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAWER_UPGRADE_T2.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAWER_UPGRADE_T3.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAWER_UPGRADE_T4.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.VOID_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.INSERT_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EXTRACT_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        blockModels.registerSimpleItemModel(ModItems.STORAGE_REMOTE.get(), Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "item/storage_remote_base"));
        itemModels.generateFlatItem(ModItems.UPGRADE_BASE.get(), ModelTemplates.FLAT_ITEM);
        blockModels.createTrivialCube(ModBlocks.DRAWER_BASE.get());
        blockModels.createTrivialCube(ModBlocks.TEST_BLOCK.get());


        for (Block drawer : ModBlocks.getAllDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(drawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(drawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(DrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, drawer);
        }

        for (Block fluidDrawer : ModBlocks.getAllFluidDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(fluidDrawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(fluidDrawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(FluidDrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, fluidDrawer);
        }

        Block compactingDrawer = ModBlocks.COMPACTING_DRAWER.get();
        var compactingModelLoc = ModelLocationUtils.getModelLocation(compactingDrawer);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(compactingDrawer, BlockModelGenerators.plainVariant(compactingModelLoc))
                        .with(PropertyDispatch.modify(CompactingDrawerBlock.FACING)
                                .select(Direction.NORTH, v -> v)
                                .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
        );
        generateDrawerItemModel(blockModels, compactingDrawer);

        Block storageInterface = ModBlocks.STORAGE_INTERFACE.get();
        Identifier unlockedModel = Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "block/storage_interface");
        Identifier lockedModel = Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "block/storage_interface_locked");

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(storageInterface, BlockModelGenerators.plainVariant(unlockedModel))
                        .with(PropertyDispatch.modify(StorageInterfaceBlock.LOCKED)
                                .select(true, v -> v.withModel(lockedModel))
                                .select(false, v -> v))
                        .with(PropertyDispatch.modify(StorageInterfaceBlock.FACING)
                                .select(Direction.NORTH, v -> v)
                                .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
        );
        generateDrawerItemModel(blockModels, storageInterface);

        Block drawerFramer = ModBlocks.DRAWER_FRAMER.get();
        var framerModelLoc = ModelLocationUtils.getModelLocation(drawerFramer);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(drawerFramer, BlockModelGenerators.plainVariant(framerModelLoc))
                        .with(PropertyDispatch.modify(DrawerFramerBlock.FACING)
                                .select(Direction.NORTH, v -> v)
                                .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
        );
        generateDrawerItemModel(blockModels, drawerFramer);


        for (Block framedDrawer : ModBlocks.getAllFramedDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(framedDrawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(framedDrawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(DrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, framedDrawer);
        }

        for (Block framedFluidDrawer : ModBlocks.getAllFramedFluidDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(framedFluidDrawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(framedFluidDrawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(FluidDrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, framedFluidDrawer);
        }

        Block framedCompactingDrawer = ModBlocks.FRAMED_COMPACTING_DRAWER.get();
        var framedCompactingModelLoc = ModelLocationUtils.getModelLocation(framedCompactingDrawer);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(framedCompactingDrawer, BlockModelGenerators.plainVariant(framedCompactingModelLoc))
                        .with(PropertyDispatch.modify(FramedCompactingDrawerBlock.FACING)
                                .select(Direction.NORTH, v -> v)
                                .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
        );
        generateDrawerItemModel(blockModels, framedCompactingDrawer);

        Block storageViewer = ModBlocks.STORAGE_VIEWER.get();
        var viewerModelLoc = ModelLocationUtils.getModelLocation(storageViewer);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(storageViewer, BlockModelGenerators.plainVariant(viewerModelLoc))
                        .with(PropertyDispatch.modify(StorageViewerBlock.FACING, StorageViewerBlock.HORIZONTAL_FACING)
                                .generate((facing, hFacing) -> {
                                    int xRot = 0;
                                    int yRot = 0;

                                    switch (facing) {
                                        case NORTH -> { xRot = 0;   yRot = 0;   }
                                        case SOUTH -> { xRot = 0;   yRot = 180; }
                                        case EAST  -> { xRot = 0;   yRot = 90;  }
                                        case WEST  -> { xRot = 0;   yRot = 270; }
                                        case UP    -> {
                                            xRot = 270;
                                            yRot = switch (hFacing) {
                                                case NORTH -> 0;
                                                case EAST  -> 90;
                                                case SOUTH -> 180;
                                                case WEST  -> 270;
                                                default    -> 0;
                                            };
                                        }
                                        case DOWN  -> {
                                            xRot = 90;
                                            yRot = switch (hFacing) {
                                                case SOUTH -> 180;
                                                case WEST  -> 270;
                                                case NORTH -> 0;
                                                case EAST  -> 90;
                                                default    -> 0;
                                            };
                                        }
                                    }

                                    return VariantMutator.X_ROT.withValue(Quadrant.values()[xRot / 90])
                                            .then(VariantMutator.Y_ROT.withValue(Quadrant.values()[yRot / 90]));
                                })
                        )
        );

        for (Block wirelessDrawer : ModBlocks.getAllWirelessDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(wirelessDrawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(wirelessDrawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(DrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST, BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, wirelessDrawer);
        }

        for (Block wirelessFluidDrawer : ModBlocks.getAllWirelessFluidDrawerBlocks()) {
            var modelLoc = ModelLocationUtils.getModelLocation(wirelessFluidDrawer);

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(wirelessFluidDrawer, BlockModelGenerators.plainVariant(modelLoc))
                            .with(PropertyDispatch.modify(FluidDrawerBlock.FACING)
                                    .select(Direction.NORTH, v -> v)
                                    .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                    .select(Direction.WEST, BlockModelGenerators.Y_ROT_270))
            );
            generateDrawerItemModel(blockModels, wirelessFluidDrawer);
        }

        Block filingCabinet = ModBlocks.FILING_CABINET.get();
        var filingCabinetClosedModelLoc = ModelLocationUtils.getModelLocation(filingCabinet);
        var filingCabinetOpenModelLoc = Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "block/filing_cabinet_open");

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(filingCabinet, BlockModelGenerators.plainVariant(filingCabinetClosedModelLoc))
                        .with(PropertyDispatch.modify(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                                .select(true, v -> v.withModel(filingCabinetOpenModelLoc))
                                .select(false, v -> v))
                        .with(PropertyDispatch.modify(FilingCabinetBlock.FACING)
                                .select(Direction.NORTH, v -> v)
                                .select(Direction.EAST,  BlockModelGenerators.Y_ROT_90)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST,  BlockModelGenerators.Y_ROT_270))
        );

        blockModels.registerSimpleItemModel(filingCabinet.asItem(), filingCabinetClosedModelLoc);
    }

    private void generateDrawerItemModel(BlockModelGenerators blockModels, Block block) {
        Identifier blockLoc = ModelLocationUtils.getModelLocation(block);
        Identifier itemLoc = Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, blockLoc.getPath().replaceFirst("block/", "item/"));
        blockModels.registerSimpleItemModel(block.asItem(), itemLoc);
    }
}