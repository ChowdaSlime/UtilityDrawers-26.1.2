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
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.JadeUI;

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

            int slotCount = drawer.getSlotCount();

            if (slotCount == 4) {
                for (int row = 0; row < 2; row++) {
                    int slotA = row * 2;
                    int slotB = row * 2 + 1;

                    var stackA = drawer.getStoredItem(slotA);
                    var stackB = drawer.getStoredItem(slotB);
                    long countA = drawer.getStoredCount(slotA);
                    long countB = drawer.getStoredCount(slotB);

                    boolean rowHasContent = false;

                    if (!stackA.isEmpty()) {
                        tooltip.add(JadeUI.item(stackA, 1.0f, String.valueOf(countA)));
                        rowHasContent = true;
                    }

                    if (!stackB.isEmpty()) {
                        if (rowHasContent) {
                            tooltip.append(Component.literal("   "));
                            tooltip.append(JadeUI.item(stackB, 1.0f, String.valueOf(countB)));
                        } else {
                            tooltip.add(JadeUI.item(stackB, 1.0f, String.valueOf(countB)));
                        }
                    }
                }
            } else {
                boolean rowStarted = false;
                for (int i = 0; i < slotCount; i++) {
                    var stack = drawer.getStoredItem(i);
                    long count = drawer.getStoredCount(i);
                    if (stack.isEmpty()) continue;

                    if (!rowStarted) {
                        tooltip.add(JadeUI.item(stack, 1.0f, String.valueOf(count)));
                        rowStarted = true;
                    } else {
                        tooltip.append(Component.literal("   "));
                        tooltip.append(JadeUI.item(stack, 1.0f, String.valueOf(count)));
                    }
                }
            }

            for (int i = 0; i < slotCount; i++) {
                var stack = drawer.getStoredItem(i);
                if (!stack.isEmpty()) {
                    tooltip.add(Component.literal("• " + stack.getHoverName().getString())
                            .withStyle(ChatFormatting.WHITE));
                }
            }

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

            for (int i = 0; i < drawer.getSlotCount(); i++) {
                FluidStack fluid = drawer.getStoredFluid(i);
                long amount = drawer.getStoredAmount(i);
                long max = drawer.getMaxCapacity(i);
                if (fluid.isEmpty()) continue;

                JadeFluidObject fluidObject = JadeFluidObject.of(
                        fluid.getFluid(),
                        amount,
                        fluid.getComponentsPatch()
                );
                tooltip.add(JadeUI.fluid(fluidObject));

                tooltip.add(Component.literal("")
                        .append(fluid.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(amount + " / " + max + " mB")
                                .withStyle(ChatFormatting.YELLOW)));
            }

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