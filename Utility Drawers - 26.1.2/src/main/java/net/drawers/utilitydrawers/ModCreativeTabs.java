package net.drawers.utilitydrawers;

import net.drawers.utilitydrawers.block.ModBlocks;
import net.drawers.utilitydrawers.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UtilityDrawers.MODID);

    public static final Supplier<CreativeModeTab> UTILITY_DRAWERS_TAB = CREATIVE_MODE_TABS.register("utility_drawers_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.TEST_ITEM.get()))
                    .title(Component.translatable("creativetab.utility_drawers_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output .accept(ModItems.DRAWER_UPGRADE_T1.get());
                        output .accept(ModItems.DRAWER_UPGRADE_T2.get());
                        output .accept(ModItems.DRAWER_UPGRADE_T3.get());
                        output .accept(ModItems.DRAWER_UPGRADE_T4.get());
                        output .accept(ModItems.VOID_UPGRADE.get());
                        output .accept(ModItems.STORAGE_REMOTE.get());

                        ModBlocks.getAllDrawerBlocks().forEach(output::accept);
                    })
                    .build());
}
