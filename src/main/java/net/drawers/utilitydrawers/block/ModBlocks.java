package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.item.CompactingDrawerBlockItem;
import net.drawers.utilitydrawers.item.DrawerBlockItem;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(UtilityDrawers.MODID);

    public static final DeferredBlock<Block> TEST_BLOCK = registerBlock(
            "test_block",
            properties -> new Block(properties.strength(4f))
    );
    public static final DeferredBlock<Block> DRAWER_BASE = registerBlock(
            "drawer_base",
            properties -> new Block(properties.strength(4f))
    );

    public static final DeferredBlock<Block> STORAGE_INTERFACE = registerBlock(
            "storage_interface",
            properties -> new StorageInterfaceBlock(
                    properties.strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> COMPACTING_DRAWER = registerCompactingDrawerBlock(
            "compacting_drawer",
            properties -> new CompactingDrawerBlock(properties.strength(1.5f))
    );

    public enum WoodType {
        OAK, SPRUCE, BIRCH, ACACIA, JUNGLE, DARK_OAK, MANGROVE, CHERRY, PALE_OAK, BAMBOO, CRIMSON, WARPED
    }

    private static final Map<WoodType, Map<Integer, DeferredBlock<Block>>> DRAWERS = new EnumMap<>(WoodType.class);

    private static final Map<Integer, DeferredBlock<Block>> FLUID_DRAWERS = new HashMap<>();

    static {
        for (WoodType wood : WoodType.values()) {
            Map<Integer, DeferredBlock<Block>> slots = new HashMap<>();
            for (int slotCount = 1; slotCount <= 4; slotCount++) {
                String name = wood.name().toLowerCase() + "_drawer_" + slotCount;
                final int sc = slotCount;
                DeferredBlock<Block> block = registerDrawerBlock(name,
                        properties -> new DrawerBlock(properties.strength(1.5f), sc));
                slots.put(slotCount, block);
            }
            DRAWERS.put(wood, slots);
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            String name = "fluid_drawer_" + slotCount;
            final int sc = slotCount;
            DeferredBlock<Block> block = registerDrawerBlock(name,
                    properties -> new FluidDrawerBlock(properties.strength(1.5f), sc));
            FLUID_DRAWERS.put(slotCount, block);
        }
    }

    public static DeferredBlock<Block> getDrawer(WoodType wood, int slotCount) {
        return DRAWERS.get(wood).get(slotCount);
    }

    public static DeferredBlock<Block> getFluidDrawer(int slotCount) {
        return FLUID_DRAWERS.get(slotCount);
    }

    public static List<Block> getAllDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (Map.Entry<WoodType, Map<Integer, DeferredBlock<Block>>> woodEntry : DRAWERS.entrySet()) {
            for (Map.Entry<Integer, DeferredBlock<Block>> slotEntry : woodEntry.getValue().entrySet()) {
                Block block = slotEntry.getValue().get();
                UtilityDrawers.LOGGER.info("Drawer block registered: {} -> {}",
                        woodEntry.getKey().name() + "_" + slotEntry.getKey(), block);
                list.add(block);
            }
        }
        return list;
    }

    public static List<Block> getAllFluidDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (Map.Entry<Integer, DeferredBlock<Block>> slotEntry : FLUID_DRAWERS.entrySet()) {
            Block block = slotEntry.getValue().get();
            UtilityDrawers.LOGGER.info("Fluid Drawer block registered: fluid_drawer_{} -> {}", slotEntry.getKey(), block);
            list.add(block);
        }
        return list;
    }

    private static <T extends Block> DeferredBlock<T> registerDrawerBlock(
            String name,
            Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new DrawerBlockItem(toReturn.value(), properties.useBlockDescriptionPrefix()));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerCompactingDrawerBlock(
            String name,
            Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new CompactingDrawerBlockItem(toReturn.value(), properties.useBlockDescriptionPrefix()));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(
            String name,
            DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name,
                properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}