package net.drawers.utilitydrawers.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public interface IFramedBlockEntity {

    BlockState getFrameState();

    BlockState getFaceState();

    CompoundTag saveDrawerData(HolderLookup.Provider provider);

    default boolean hasFraming() {
        return getFrameState() != null || getFaceState() != null;
    }
}