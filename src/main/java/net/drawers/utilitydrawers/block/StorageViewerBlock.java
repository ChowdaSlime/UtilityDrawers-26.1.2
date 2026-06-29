package net.drawers.utilitydrawers.block;


import com.mojang.serialization.MapCodec;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageViewerBlockEntity;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;




public class StorageViewerBlock extends BaseEntityBlock {


    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.FACING;
    public static final EnumProperty<Direction> HORIZONTAL_FACING =
            EnumProperty.create("horizontal_facing", Direction.class, Direction.Plane.HORIZONTAL);



    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0,  0, 16, 16,  2);
    private static final VoxelShape SHAPE_WEST  = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST  = Block.box(0,  0, 0,  2, 16, 16);
    private static final VoxelShape SHAPE_UP    = Block.box(0,  0, 0, 16,  2, 16);
    private static final VoxelShape SHAPE_DOWN  = Block.box(0, 14, 0, 16, 16, 16);


    public StorageViewerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HORIZONTAL_FACING, Direction.NORTH));
    }


    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(StorageViewerBlock::new);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HORIZONTAL_FACING);
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();

        BlockPos supportPos = ctx.getClickedPos().relative(face.getOpposite());
        BlockState supportState = ctx.getLevel().getBlockState(supportPos);

        if (!supportState.isFaceSturdy(ctx.getLevel(), supportPos, face)) {
            return null;
        }
        Direction horizontalFacing = ctx.getHorizontalDirection().getOpposite();

        return defaultBlockState()
                .setValue(FACING, face)
                .setValue(HORIZONTAL_FACING, horizontalFacing);
    }


    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == state.getValue(FACING).getOpposite()) {
            if (!neighbourState.isFaceSturdy(level, neighbourPos, state.getValue(FACING))) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
        };
    }


    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!level.isClientSide()) {
                ItemStack held = player.getMainHandItem();
                if (held.getItem() instanceof StorageRemoteItem remote && StorageRemoteItem.isLinkMode(held)) {
                    return InteractionResult.PASS;
                }
            }

            if (be instanceof StorageViewerBlockEntity viewer) {
                BlockPos interfacePos = viewer.getStorageInterfacePos();
                StorageInterfaceBlockEntity storageInterface = null;
                if (interfacePos != null) {
                    BlockEntity interfaceBe = level.getBlockEntity(interfacePos);
                    if (interfaceBe instanceof StorageInterfaceBlockEntity sie) {
                        storageInterface = sie;
                    }
                }

                if (storageInterface != null) {
                    storageInterface.refreshNetworkNodes();
                }

                player.openMenu(viewer, buf -> {
                    buf.writeBoolean(interfacePos != null);
                    if (interfacePos != null) {
                        buf.writeBlockPos(interfacePos);
                    }
                    buf.writeBlockPos(pos);
                });
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageViewerBlockEntity(pos, state);
    }


    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide()) return;


        Direction facing = state.getValue(FACING);
        BlockPos interfacePos = pos.relative(facing.getOpposite());


        BlockEntity be = level.getBlockEntity(interfacePos);
        if (be instanceof StorageInterfaceBlockEntity) {
            if (level.getBlockEntity(pos) instanceof StorageViewerBlockEntity viewer) {
                viewer.setStorageInterfacePos(interfacePos);
            }
        }
    }
}
