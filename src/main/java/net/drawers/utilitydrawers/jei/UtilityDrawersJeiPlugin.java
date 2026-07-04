package net.drawers.utilitydrawers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.menu.UpgradeConfigScreen;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class UtilityDrawersJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(UpgradeConfigScreen.class, new UpgradeConfigGhostIngredientHandler());
    }
}