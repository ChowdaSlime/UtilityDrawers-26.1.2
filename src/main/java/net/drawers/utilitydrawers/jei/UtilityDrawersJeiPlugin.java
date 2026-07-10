package net.drawers.utilitydrawers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.menu.UpgradeConfigScreen;
import net.drawers.utilitydrawers.menu.WirelessDrawerScreen;
import net.drawers.utilitydrawers.menu.WirelessFluidDrawerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

@JeiPlugin
public class UtilityDrawersJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "jei_plugin");
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
    }
}