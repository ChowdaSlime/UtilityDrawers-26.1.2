package net.chowdaslime.utilitydrawers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.chowdaslime.utilitydrawers.menu.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

@JeiPlugin
public class UtilityDrawersJeiPlugin implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void setFilterText(String text) {
        if (runtime != null) {
            runtime.getIngredientFilter().setFilterText(text);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(UpgradeConfigScreen.class, new UpgradeConfigGhostIngredientHandler());

        registration.addGuiContainerHandler(WirelessDrawerScreen.class, new IGuiContainerHandler<WirelessDrawerScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(WirelessDrawerScreen containerScreen) {
                return containerScreen.getExtraGuiAreas();
            }
        });

        registration.addGuiContainerHandler(WirelessFluidDrawerScreen.class, new IGuiContainerHandler<WirelessFluidDrawerScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(WirelessFluidDrawerScreen containerScreen) {
                return containerScreen.getExtraGuiAreas();
            }
        });

        registration.addGuiContainerHandler(StorageViewerScreen.class, new IGuiContainerHandler<StorageViewerScreen>() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory builder, StorageViewerScreen screen, double mouseX, double mouseY) {

                int cellIndex = screen.getCellAt((int) mouseX, (int) mouseY);
                if (cellIndex < 0 || cellIndex >= screen.filteredSlots.size()) {
                    return Optional.empty();
                }

                StorageViewerMenu.NetworkSlot ns = screen.filteredSlots.get(cellIndex);
                if (ns.isFluid()) {
                    return Optional.empty();
                }

                ItemStack stack = ns.stack().copyWithCount(1);

                int gx = screen.getLeftPos() + 8;
                int gy = screen.getTopPos() + 17;
                int col = ((int) mouseX - gx) / 18;
                int row = ((int) mouseY - gy) / 18;
                Rect2i area = new Rect2i(gx + col * 18, gy + row * 18, 16, 16);

                return builder.createBuilder(stack).buildWithArea(area);
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new CraftingViewerTransferHandler(ModMenuTypes.CRAFTING_STORAGE_VIEWER_MENU.get(), registration.getTransferHelper()),
                RecipeTypes.CRAFTING
        );
    }
}