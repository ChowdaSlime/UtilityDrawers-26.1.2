package net.drawers.utilitydrawers.jade;

import net.drawers.utilitydrawers.block.*;
import net.drawers.utilitydrawers.block.entity.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

@WailaPlugin
public class UtilityDrawersJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DrawerProvider.INSTANCE, DrawerBlock.class);
        registration.registerBlockComponent(FluidDrawerProvider.INSTANCE, FluidDrawerBlock.class);
        registration.registerBlockComponent(StorageInterfaceProvider.INSTANCE, StorageInterfaceBlock.class);

        registration.registerBlockIcon(FramedDrawerIconProvider.INSTANCE, FramedDrawerBlock.class);
        registration.registerBlockIcon(FramedDrawerIconProvider.INSTANCE, FramedFluidDrawerBlock.class);
        registration.registerBlockIcon(FramedDrawerIconProvider.INSTANCE, FramedCompactingDrawerBlock.class);
    }


    public static class DrawerProvider implements IBlockComponentProvider {
        public static final DrawerProvider INSTANCE = new DrawerProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof DrawerBlockEntity drawer)) return;

            if (drawer.isLocked()) {
                tooltip.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
            }

            if (drawer instanceof WirelessDrawerBlockEntity wireless) {
                var key = wireless.getNetworkKey();
                if (!key.isPublic()) {
                    key.owner().ifPresent(uuid -> {
                        String name = resolveOwnerName(accessor, uuid);
                        tooltip.add(Component.literal("Owned By: " + name).withStyle(ChatFormatting.GRAY));
                    });
                }
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

            if (drawer instanceof WirelessFluidDrawerBlockEntity wireless) {
                var key = wireless.getNetworkKey();
                if (!key.isPublic()) {
                    key.owner().ifPresent(uuid -> {
                        String name = resolveOwnerName(accessor, uuid);
                        tooltip.add(Component.literal("Owned By: " + name).withStyle(ChatFormatting.GRAY));
                    });
                }
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

    public static class FramedDrawerIconProvider implements IBlockComponentProvider {
        public static final FramedDrawerIconProvider INSTANCE = new FramedDrawerIconProvider();

        @Override
        public @Nullable Element getIcon(BlockAccessor accessor, IPluginConfig config, @Nullable Element currentIcon) {
            if (!(accessor.getBlockEntity() instanceof IFramedBlockEntity framed)) {
                return currentIcon;
            }

            ItemStack stack = new ItemStack(accessor.getBlock());
            CompoundTag tag = framed.saveDrawerData(accessor.getLevel().registryAccess());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return JadeUI.item(stack);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof IFramedBlockEntity framed) {
                BlockState faceState = framed.getFaceState();

                if (faceState != null) {
                    ItemStack faceStack = new ItemStack(faceState.getBlock());
                    if (!faceStack.isEmpty()) {
                        tooltip.add(JadeUI.item(faceStack));
                        tooltip.append(JadeUI.text(Component.literal(" Front Face")));
                    }
                }
            }
        }

        @Override
        public Identifier getUid() {
            return Identifier.fromNamespaceAndPath("utilitydrawers", "framed_drawer_icon");
        }
    }

    private static String resolveOwnerName(BlockAccessor accessor, java.util.UUID uuid) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) {
            var playerInfo = connection.getPlayerInfo(uuid);
            if (playerInfo != null) {
                return playerInfo.getProfile().name();
            }
        }
        return uuid.toString();
    }
}