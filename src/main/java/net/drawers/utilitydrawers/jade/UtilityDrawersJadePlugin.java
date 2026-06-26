package net.drawers.utilitydrawers.jade;

import net.drawers.utilitydrawers.block.DrawerBlock;
import net.drawers.utilitydrawers.block.FluidDrawerBlock;
import net.drawers.utilitydrawers.block.StorageInterfaceBlock;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class UtilityDrawersJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DrawerProvider.INSTANCE, DrawerBlock.class);
        registration.registerBlockComponent(FluidDrawerProvider.INSTANCE, FluidDrawerBlock.class);
        registration.registerBlockComponent(StorageInterfaceProvider.INSTANCE, StorageInterfaceBlock.class);
    }

    public static class DrawerProvider implements IBlockComponentProvider {
        public static final DrawerProvider INSTANCE = new DrawerProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof DrawerBlockEntity drawer)) return;

            if (drawer.isLocked()) {
                tooltip.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public Identifier getUid() {
            return Identifier.fromNamespaceAndPath("utilitydrawers", "drawer");
        }
    }

    public static class FluidDrawerProvider implements IBlockComponentProvider {
        public static final FluidDrawerProvider INSTANCE = new FluidDrawerProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof FluidDrawerBlockEntity drawer)) return;

            if (drawer.isLocked()) {
                tooltip.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public Identifier getUid() {
            return Identifier.fromNamespaceAndPath("utilitydrawers", "fluid_drawer");
        }
    }

    public static class StorageInterfaceProvider implements IBlockComponentProvider {
        public static final StorageInterfaceProvider INSTANCE = new StorageInterfaceProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof StorageInterfaceBlockEntity interfaceEntity)) return;

            var drawers = interfaceEntity.getConnectedDrawers();
            tooltip.add(Component.literal("Connected Drawers: ")
                    .withStyle(ChatFormatting.GRAY)
                    .copy()
                    .append(Component.literal(String.valueOf(drawers.size()))
                            .withStyle(ChatFormatting.YELLOW)));
        }

        @Override
        public Identifier getUid() {
            return Identifier.fromNamespaceAndPath("utilitydrawers", "storage_interface");
        }
    }
}