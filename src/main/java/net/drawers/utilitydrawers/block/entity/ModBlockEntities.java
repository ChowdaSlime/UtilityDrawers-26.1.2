package net.drawers.utilitydrawers.block.entity;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UtilityDrawers.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrawerBlockEntity>> DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("drawer_block_entity", () -> {
                return new BlockEntityType<>(DrawerBlockEntity::new, Set.copyOf(ModBlocks.getAllDrawerBlocks()));
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidDrawerBlockEntity>> FLUID_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("fluid_drawer_block_entity", () -> {
                return new BlockEntityType<>(FluidDrawerBlockEntity::new, Set.copyOf(ModBlocks.getAllFluidDrawerBlocks()));
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompactingDrawerBlockEntity>> COMPACTING_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("compacting_drawer_block_entity", () ->
                    new BlockEntityType<>(CompactingDrawerBlockEntity::new, Set.of(ModBlocks.COMPACTING_DRAWER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FramedDrawerBlockEntity>> FRAMED_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("framed_drawer_block_entity", () ->
                    new BlockEntityType<>(FramedDrawerBlockEntity::new, Set.copyOf(ModBlocks.getAllFramedDrawerBlocks())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FramedFluidDrawerBlockEntity>> FRAMED_FLUID_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("framed_fluid_drawer_block_entity", () ->
                    new BlockEntityType<>(FramedFluidDrawerBlockEntity::new, Set.copyOf(ModBlocks.getAllFramedFluidDrawerBlocks())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FramedCompactingDrawerBlockEntity>> FRAMED_COMPACTING_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("framed_compacting_drawer_block_entity", () ->
                    new BlockEntityType<>(FramedCompactingDrawerBlockEntity::new, Set.of(ModBlocks.FRAMED_COMPACTING_DRAWER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessDrawerBlockEntity>> WIRELESS_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wireless_drawer_block_entity", () ->
                    new BlockEntityType<>(WirelessDrawerBlockEntity::new,
                            Set.copyOf(ModBlocks.getAllWirelessDrawerBlocks())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessFluidDrawerBlockEntity>> WIRELESS_FLUID_DRAWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wireless_fluid_drawer_block_entity", () ->
                    new BlockEntityType<>(WirelessFluidDrawerBlockEntity::new,
                            Set.copyOf(ModBlocks.getAllWirelessFluidDrawerBlocks())));


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageInterfaceBlockEntity>> STORAGE_INTERFACE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("storage_interface_block_entity", () ->
                    new BlockEntityType<>(StorageInterfaceBlockEntity::new, Set.of(ModBlocks.STORAGE_INTERFACE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrawerFramerBlockEntity>> DRAWER_FRAMER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("drawer_framer_block_entity", () ->
                    new BlockEntityType<>(DrawerFramerBlockEntity::new, Set.of(ModBlocks.DRAWER_FRAMER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageViewerBlockEntity>> STORAGE_VIEWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("storage_viewer_block_entity", () ->
                    new BlockEntityType<>(StorageViewerBlockEntity::new, Set.of(ModBlocks.STORAGE_VIEWER.get())));



    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}