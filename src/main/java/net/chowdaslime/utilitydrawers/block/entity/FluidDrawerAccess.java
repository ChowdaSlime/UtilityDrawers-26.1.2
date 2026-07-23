package net.chowdaslime.utilitydrawers.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;

public interface FluidDrawerAccess {
    int getSlotCount();
    FluidStack getStoredFluid(int slot);
    boolean isSlotEmpty(int slot);
    FluidStack insertFluidIntoSlot(int slot, FluidStack stack, boolean simulate);
    FluidStack extractFluid(int slot, int amount, boolean simulate);
}