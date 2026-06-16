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
                return new BlockEntityType<>(
                        DrawerBlockEntity::new,
                        Set.copyOf(ModBlocks.getAllDrawerBlocks()));
            });



    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageInterfaceBlockEntity>> STORAGE_INTERFACE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("storage_interface_entity", () ->
                    new BlockEntityType<>(
                            StorageInterfaceBlockEntity::new, Set.of(ModBlocks.STORAGE_INTERFACE.get())));



    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}