package net.chowdaslime.utilitydrawers;

import net.chowdaslime.utilitydrawers.attachment.ModAttachments;
import net.chowdaslime.utilitydrawers.block.ModBlocks;
import net.chowdaslime.utilitydrawers.block.entity.ModBlockEntities;
import net.chowdaslime.utilitydrawers.data.ModDataComponents;
import net.chowdaslime.utilitydrawers.item.ModItems;
import net.chowdaslime.utilitydrawers.menu.ModMenuTypes;
import net.chowdaslime.utilitydrawers.recipe.ModRecipeSerializers;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(UtilityDrawers.MODID)
public class UtilityDrawers {

    public static final String MODID = "utilitydrawers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UtilityDrawers(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);



        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, UtilityDrawersConfig.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

}