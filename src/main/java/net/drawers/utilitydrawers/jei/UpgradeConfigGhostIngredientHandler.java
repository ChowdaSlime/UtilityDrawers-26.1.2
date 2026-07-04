package net.drawers.utilitydrawers.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.drawers.utilitydrawers.menu.UpgradeConfigMenu;
import net.drawers.utilitydrawers.menu.UpgradeConfigScreen;
import net.drawers.utilitydrawers.network.SetFilterSlotPayload;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class UpgradeConfigGhostIngredientHandler implements IGhostIngredientHandler<UpgradeConfigScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(UpgradeConfigScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        UpgradeConfigMenu menu = screen.getMenu();

        for (int i = 0; i < 9; i++) {
            Slot slot = menu.slots.get(i);
            int x = screen.getLeftPos() + slot.x;
            int y = screen.getTopPos() + slot.y;
            Rect2i area = new Rect2i(x, y, 16, 16);
            int slotIndex = i;

            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ing) {
                    Object rawIngredient = ingredient.getIngredient();
                    ItemStack stack;

                    if (rawIngredient instanceof ItemStack itemStack) {
                        stack = itemStack.copyWithCount(1);
                    }  else if (rawIngredient instanceof FluidStack fluidStack) {
                        stack = fluidStack.getFluidType().getBucket(fluidStack);
                    } else {
                        return;
                    }

                    if (stack.isEmpty()) return;

                    ClientPacketDistributor.sendToServer(new SetFilterSlotPayload(menu.containerId, slotIndex, stack));
                }
            });
        }

        return targets;
    }

    @Override
    public void onComplete() {
    }
}