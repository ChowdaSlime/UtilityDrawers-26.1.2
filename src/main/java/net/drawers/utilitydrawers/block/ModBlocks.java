package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Function;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(UtilityDrawers.MODID);

    public static final DeferredBlock<Block> TEST_BLOCK = registerBlock(
            "test_block", properties -> new Block(properties.strength(4f)));

    public static final DeferredBlock<Block> DRAWER_BASE = registerBlock(
            "drawer_base", properties -> new Block(properties.strength(4f)));

    public static final DeferredBlock<Block> STORAGE_INTERFACE = registerBlock(
            "storage_interface",
            properties -> new StorageInterfaceBlock(
                    properties.strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> COMPACTING_DRAWER = registerCompactingDrawerBlock(
            "compacting_drawer",
            properties -> new CompactingDrawerBlock(properties.strength(1.5f)));

    public static final DeferredBlock<Block> FRAMED_COMPACTING_DRAWER = registerCompactingDrawerBlock(
            "framed_compacting_drawer",
            properties -> new FramedCompactingDrawerBlock(properties.strength(1.5f).noOcclusion()));

    public static final DeferredBlock<Block> DRAWER_FRAMER = registerBlock(
            "drawer_framer",
            properties -> new DrawerFramerBlock(
                    properties.strength(3.0F, 6.0F).sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> STORAGE_VIEWER = registerBlock(
            "storage_viewer",
            properties -> new StorageViewerBlock(
                    properties.strength(2.0F, 4.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops()));


    public enum WoodType {
        OAK, SPRUCE, BIRCH, ACACIA, JUNGLE, DARK_OAK, MANGROVE, CHERRY, PALE_OAK, BAMBOO, CRIMSON, WARPED
    }

    private static final Map<WoodType, Map<Integer, DeferredBlock<Block>>> DRAWERS = new EnumMap<>(WoodType.class);
    private static final Map<Integer, DeferredBlock<Block>> FLUID_DRAWERS = new HashMap<>();
    private static final Map<Integer, DeferredBlock<Block>> FRAMED_DRAWERS = new HashMap<>();
    private static final Map<Integer, DeferredBlock<Block>> FRAMED_FLUID_DRAWERS = new HashMap<>();
    private static final Map<Integer, DeferredBlock<Block>> WIRELESS_DRAWERS = new HashMap<>();
    private static final Map<Integer, DeferredBlock<Block>> WIRELESS_FLUID_DRAWERS = new HashMap<>();

    static {
        for (WoodType wood : WoodType.values()) {
            Map<Integer, DeferredBlock<Block>> slots = new HashMap<>();
            for (int slotCount = 1; slotCount <= 4; slotCount++) {
                String name = wood.name().toLowerCase() + "_drawer_" + slotCount;
                final int sc = slotCount;
                slots.put(slotCount, registerDrawerBlock(name,
                        properties -> new DrawerBlock(properties.strength(1.5f), sc)));
            }
            DRAWERS.put(wood, slots);
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            final int sc = slotCount;
            FLUID_DRAWERS.put(slotCount, registerDrawerBlock("fluid_drawer_" + slotCount,
                    properties -> new FluidDrawerBlock(properties.strength(1.5f), sc)));
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            final int sc = slotCount;
            FRAMED_DRAWERS.put(slotCount, registerDrawerBlock("framed_drawer_" + slotCount,
                    properties -> new FramedDrawerBlock(properties.strength(1.5f).noOcclusion(), sc)));
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            final int sc = slotCount;
            FRAMED_FLUID_DRAWERS.put(slotCount, registerDrawerBlock("framed_fluid_drawer_" + slotCount,
                    properties -> new FramedFluidDrawerBlock(properties.strength(1.5f).noOcclusion(), sc)));
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            final int sc = slotCount;
            WIRELESS_DRAWERS.put(slotCount, registerWirelessDrawerBlock("wireless_drawer_" + slotCount,
                    properties -> new WirelessDrawerBlock(properties.strength(1.5f), sc)));
        }

        for (int slotCount = 1; slotCount <= 4; slotCount++) {
            final int sc = slotCount;
            WIRELESS_FLUID_DRAWERS.put(slotCount, registerWirelessFluidDrawerBlock("wireless_fluid_drawer_" + slotCount,
                    properties -> new WirelessFluidDrawerBlock(properties.strength(1.5f), sc)));
        }

    }

    public static DeferredBlock<Block> getDrawer(WoodType wood, int slotCount) {
        return DRAWERS.get(wood).get(slotCount);
    }

    public static DeferredBlock<Block> getFluidDrawer(int slotCount) {
        return FLUID_DRAWERS.get(slotCount);
    }

    public static DeferredBlock<Block> getFramedDrawer(int slotCount) {
        return FRAMED_DRAWERS.get(slotCount);
    }

    public static DeferredBlock<Block> getFramedFluidDrawer(int slotCount) {
        return FRAMED_FLUID_DRAWERS.get(slotCount);
    }

    public static List<Block> getAllDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var woodEntry : DRAWERS.entrySet())
            for (var slotEntry : woodEntry.getValue().entrySet())
                list.add(slotEntry.getValue().get());
        return list;
    }

    public static List<Block> getAllFluidDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var entry : FLUID_DRAWERS.entrySet())
            list.add(entry.getValue().get());
        return list;
    }

    public static List<Block> getAllFramedDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var entry : FRAMED_DRAWERS.entrySet())
            list.add(entry.getValue().get());
        return list;
    }

    public static List<Block> getAllFramedFluidDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var entry : FRAMED_FLUID_DRAWERS.entrySet())
            list.add(entry.getValue().get());
        return list;
    }

    public static DeferredBlock<Block> getWirelessDrawer(int slotCount) {
        return WIRELESS_DRAWERS.get(slotCount);
    }

    public static DeferredBlock<Block> getWirelessFluidDrawer(int slotCount) {
        return WIRELESS_FLUID_DRAWERS.get(slotCount);
    }

    public static List<Block> getAllWirelessDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var entry : WIRELESS_DRAWERS.entrySet())
            list.add(entry.getValue().get());
        return list;
    }

    public static List<Block> getAllWirelessFluidDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (var entry : WIRELESS_FLUID_DRAWERS.entrySet())
            list.add(entry.getValue().get());
        return list;
    }

    private static <T extends Block> DeferredBlock<T> registerDrawerBlock(
            String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new DrawerBlockItem(toReturn.value(), properties.useBlockDescriptionPrefix()));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerCompactingDrawerBlock(
            String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new CompactingDrawerBlockItem(toReturn.value(), properties.useBlockDescriptionPrefix()));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerWirelessDrawerBlock(
            String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new WirelessDrawerBlockItem(
                        toReturn.value(), properties.useBlockDescriptionPrefix()
                ));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerWirelessFluidDrawerBlock(
            String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new WirelessFluidDrawerBlockItem(
                        toReturn.value(), properties.useBlockDescriptionPrefix()
                ));
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new BlockItem(toReturn.value(), properties.useBlockDescriptionPrefix()));
        return toReturn;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}