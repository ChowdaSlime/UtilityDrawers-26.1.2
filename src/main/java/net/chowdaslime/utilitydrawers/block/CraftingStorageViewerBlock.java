package net.chowdaslime.utilitydrawers.block;

import com.mojang.serialization.MapCodec;
import net.chowdaslime.utilitydrawers.block.entity.CraftingStorageViewerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingStorageViewerBlock extends StorageViewerBlock {
    public static final MapCodec<CraftingStorageViewerBlock> CODEC = simpleCodec(CraftingStorageViewerBlock::new);

    public CraftingStorageViewerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends StorageViewerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingStorageViewerBlockEntity(pos, state);
    }
}