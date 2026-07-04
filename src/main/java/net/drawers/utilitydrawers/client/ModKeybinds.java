package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class ModKeybinds {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "main")
    );

    public static final KeyMapping OPEN_UPGRADE_CONFIG = new KeyMapping(
            "key.utilitydrawers.open_upgrade_config",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_UPGRADE_CONFIG);
    }
}
