package net.drawers.utilitydrawers.item;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UtilityDrawers.MODID);

    public static final DeferredItem<Item> TEST_ITEM = ITEMS.registerSimpleItem("test_item");

    public static final DeferredItem<DrawerUpgradeItem> DRAWER_UPGRADE_T1 =
            ITEMS.registerItem("drawer_upgrade_t1", properties -> new DrawerUpgradeItem(properties, 1));
    public static final DeferredItem<DrawerUpgradeItem> DRAWER_UPGRADE_T2 =
            ITEMS.registerItem("drawer_upgrade_t2", properties -> new DrawerUpgradeItem(properties, 2));
    public static final DeferredItem<DrawerUpgradeItem> DRAWER_UPGRADE_T3 =
            ITEMS.registerItem("drawer_upgrade_t3", properties -> new DrawerUpgradeItem(properties, 3));
    public static final DeferredItem<DrawerUpgradeItem> DRAWER_UPGRADE_T4 =
            ITEMS.registerItem("drawer_upgrade_t4", properties -> new DrawerUpgradeItem(properties, 4));
    public static final DeferredItem<VoidUpgradeItem> VOID_UPGRADE =
            ITEMS.registerItem("void_upgrade", properties -> new VoidUpgradeItem(properties));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}