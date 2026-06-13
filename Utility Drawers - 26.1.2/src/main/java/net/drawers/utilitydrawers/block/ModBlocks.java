package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.item.DrawerBlockItem;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.drawers.utilitydrawers.block.DrawerBlock;

import java.util.ArrayList;
import java.util.EnumMap;
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

    public enum WoodType {
        OAK,
        SPRUCE,
        BIRCH,
        ACACIA,
        JUNGLE,
        DARK_OAK,
        MANGROVE,
        CHERRY,
        PALE_OAK,
        BAMBOO,
        CRIMSON,
        WARPED
    }


    private static final Map<WoodType, Map<Integer, DeferredBlock<Block>>> DRAWERS =
            new EnumMap<>(WoodType.class);

    static {
        for (WoodType wood : WoodType.values()) {
            Map<Integer, DeferredBlock<Block>> slots = new java.util.HashMap<>();
            for (int slotCount = 1; slotCount <= 4; slotCount++) {
                String name = wood.name().toLowerCase() + "_drawer_" + slotCount;
                final int sc = slotCount;
                DeferredBlock<Block> block = registerDrawerBlock(name,
                        properties -> new DrawerBlock(properties.strength(2.5f), sc));
                slots.put(slotCount, block);
            }
            DRAWERS.put(wood, slots);
        }
    }

    public static DeferredBlock<Block> getDrawer(WoodType wood, int slotCount) {
        return DRAWERS.get(wood).get(slotCount);
    }

    public static List<Block> getAllDrawerBlocks() {
        List<Block> list = new ArrayList<>();
        for (Map<Integer, DeferredBlock<Block>> slots : DRAWERS.values()) {
            for (DeferredBlock<Block> block : slots.values()) {
                list.add(block.get());
            }
        }
        return list;
    }

    private static <T extends Block> DeferredBlock<T> registerDrawerBlock(
            String name,
            Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        ModItems.ITEMS.registerItem(name,
                properties -> new DrawerBlockItem(toReturn.get(), properties.useBlockDescriptionPrefix()));
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
                properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}