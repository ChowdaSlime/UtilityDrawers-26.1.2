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

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}