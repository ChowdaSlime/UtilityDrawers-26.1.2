package net.drawers.utilitydrawers.block.entity;

import net.minecraft.world.level.block.state.BlockState;

public interface IFramedBlockEntity {

    BlockState getFrameState();

    BlockState getFaceState();

    default boolean hasFraming() {
        return getFrameState() != null || getFaceState() != null;
    }
}