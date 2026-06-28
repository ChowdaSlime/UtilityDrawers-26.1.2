package net.drawers.utilitydrawers.menu;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, UtilityDrawers.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<DrawerMenu>> DRAWER_MENU =
            MENU_TYPES.register("drawer_menu",
                    () -> IMenuTypeExtension.create(DrawerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FluidDrawerMenu>> FLUID_DRAWER_MENU =
            MENU_TYPES.register("fluid_drawer_menu",
                    () -> IMenuTypeExtension.create(FluidDrawerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CompactingDrawerMenu>> COMPACTING_DRAWER_MENU =
            MENU_TYPES.register("compacting_drawer_menu",
                    () -> IMenuTypeExtension.create(CompactingDrawerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageInterfaceMenu>> STORAGE_INTERFACE_MENU =
            MENU_TYPES.register("storage_interface_menu",
                    () -> IMenuTypeExtension.create(StorageInterfaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DrawerFramerMenu>> DRAWER_FRAMER_MENU =
            MENU_TYPES.register("drawer_framer_menu",
                    () -> IMenuTypeExtension.create(DrawerFramerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageViewerMenu>> STORAGE_VIEWER_MENU =
            MENU_TYPES.register("storage_viewer_menu",
                    () -> IMenuTypeExtension.create(StorageViewerMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}