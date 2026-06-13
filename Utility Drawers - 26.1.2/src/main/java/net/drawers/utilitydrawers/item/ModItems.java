package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UtilityDrawers.MODID);

    public static final DeferredItem<Item> TEST_ITEM = ITEMS.registerSimpleItem("test_item");


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}