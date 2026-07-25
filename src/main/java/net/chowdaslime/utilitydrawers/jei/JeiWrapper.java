package net.chowdaslime.utilitydrawers.jei;

import net.neoforged.fml.ModList;

public class JeiWrapper {
    public static void setFilterText(String text) {
        if (ModList.get().isLoaded("jei")) {
            UtilityDrawersJeiPlugin.setFilterText(text);
        }
    }
}