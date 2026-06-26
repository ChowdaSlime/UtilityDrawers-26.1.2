package net.drawers.utilitydrawers.block;

import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FramedFluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.SlotCountProvider;
import net.drawers.utilitydrawers.item.DrawerUpgradeItem;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.drawers.utilitydrawers.item.VoidUpgradeItem;
import net.drawers.utilitydrawers.menu.FluidDrawerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class FramedFluidDrawerBlock extends Block implements SlotCountProvider, EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final int slotCount;

    public FramedFluidDrawerBlock(Properties properties, int slotCount) {
        super(properties);
        this.slotCount = slotCount;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedFluidDrawerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleInteraction(state, level, pos, player, hit, stack, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleInteraction(state, level, pos, player, hit, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }

    private InteractionResult handleInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, ItemStack handStack, InteractionHand hand) {
        if (handStack.getItem() instanceof StorageRemoteItem) {
            return InteractionResult.PASS;
        }

        if (!handStack.isEmpty() && (handStack.getItem() instanceof DrawerUpgradeItem || handStack.getItem() instanceof VoidUpgradeItem)) {
            if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity drawer) {
                if (drawer.insertUpgrade(handStack)) {
                    if (!level.isClientSide() && !player.isCreative()) {
                        handStack.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                }
            }
        }

        if (hit.getDirection() != state.getValue(FACING)) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof FluidDrawerBlockEntity && player instanceof ServerPlayer serverPlayer) {
                    Component title = Component.literal("Fluid Drawer");
                    serverPlayer.openMenu(new MenuProvider() {
                        @Override public Component getDisplayName() { return title; }
                        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new FluidDrawerMenu(id, inv, be);
                        }
                    }, buf -> buf.writeBlockPos(pos));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof FluidDrawerBlockEntity drawer) {
            int targetSlot = getTargetSlot(hit.getLocation(), state, drawer.getSlotCount());

            if (targetSlot < 0) {
                if (player instanceof ServerPlayer serverPlayer) {
                    Component title = Component.literal("Fluid Drawer");
                    serverPlayer.openMenu(new MenuProvider() {
                        @Override public Component getDisplayName() { return title; }
                        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                            return new FluidDrawerMenu(id, inv, drawer);
                        }
                    }, buf -> buf.writeBlockPos(pos));
                }
                return InteractionResult.SUCCESS;
            }

            if (!handStack.isEmpty()) {
                ItemAccess access = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
                ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);

                if (fluidHandler != null) {
                    boolean interacted = false;

                    FluidResource heldResource = null;
                    int heldAmount = 0;
                    for (int i = 0; i < fluidHandler.size(); i++) {
                        FluidResource r = fluidHandler.getResource(i);
                        if (!r.isEmpty()) {
                            heldResource = r;
                            heldAmount += fluidHandler.getAmountAsInt(i);
                        }
                    }

                    if (heldResource != null && heldAmount > 0) {
                        FluidStack simInsert = heldResource.toStack(heldAmount);

                        if (drawer.isLocked() && !drawer.hasTemplate(targetSlot)) {
                            drawer.setTemplate(targetSlot, simInsert);
                        }

                        FluidStack remainder = drawer.insertFluidIntoSlot(targetSlot, simInsert, true);
                        int amountToDrain = heldAmount - remainder.getAmount();

                        if (amountToDrain > 0) {
                            FluidResource res = heldResource;
                            try (Transaction tx = Transaction.openRoot()) {
                                int drained = fluidHandler.extract(res, amountToDrain, tx);
                                if (drained > 0) {
                                    drawer.insertFluidIntoSlot(targetSlot, res.toStack(drained), false);
                                    tx.commit();
                                    interacted = true;
                                }
                            }
                            if (interacted) {
                                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                            }
                        }
                    }

                    if (!interacted && !drawer.isSlotEmpty(targetSlot)) {
                        FluidStack storedFluid = drawer.getStoredFluid(targetSlot);
                        long storedAmount = drawer.getStoredAmount(targetSlot);

                        if (storedAmount > 0) {
                            FluidResource storedResource = FluidResource.of(storedFluid);
                            int maxExtractable = (int) Math.min(Integer.MAX_VALUE, storedAmount);

                            try (Transaction tx = Transaction.openRoot()) {
                                int simFill = fluidHandler.insert(storedResource, maxExtractable, tx);
                                if (simFill > 0) {
                                    FluidStack actuallyExtracted = drawer.extractFluid(targetSlot, simFill, false);
                                    if (actuallyExtracted.getAmount() > 0) {
                                        tx.commit();
                                        interacted = true;
                                        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                                    }
                                }
                            }
                        }
                    }

                    if (interacted) {
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack dropStack = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluidDrawerBlockEntity drawerEntity) {
            CompoundTag tag = drawerEntity.saveDrawerData(builder.getLevel().registryAccess());
            dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return List.of(dropStack);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FluidDrawerBlockEntity drawerEntity) {
                drawerEntity.loadContentsFromTag(customData.copyTag());
            }
        }
    }

    public int getTargetSlot(Vec3 hitPos, BlockState state, int slotCount) {
        Direction facing = state.getValue(FACING);
        double u, v;
        switch (facing) {
            case NORTH -> { u = 1.0 - (hitPos.x - Math.floor(hitPos.x)); v = 1.0 - (hitPos.y - Math.floor(hitPos.y)); }
            case SOUTH -> { u = hitPos.x - Math.floor(hitPos.x);         v = 1.0 - (hitPos.y - Math.floor(hitPos.y)); }
            case WEST  -> { u = hitPos.z - Math.floor(hitPos.z);         v = 1.0 - (hitPos.y - Math.floor(hitPos.y)); }
            case EAST  -> { u = 1.0 - (hitPos.z - Math.floor(hitPos.z)); v = 1.0 - (hitPos.y - Math.floor(hitPos.y)); }
            default    -> { return 0; }
        }
        final double EDGE = 1.0 / 16.0;
        final double TRIM = 1.0 / 16.0;
        double halfLow  = 0.5 - TRIM / 2.0;
        double halfHigh = 0.5 + TRIM / 2.0;
        boolean inTopZone    = v >= EDGE && v < halfLow;
        boolean inBottomZone = v > halfHigh && v <= (1.0 - EDGE);
        boolean inLeftZone   = u >= EDGE && u < halfLow;
        boolean inRightZone  = u > halfHigh && u <= (1.0 - EDGE);
        boolean inUEdge      = u >= EDGE && u <= (1.0 - EDGE);
        return switch (slotCount) {
            case 1 -> (u >= EDGE && u <= (1.0 - EDGE) && v >= EDGE && v <= (1.0 - EDGE)) ? 0 : -1;
            case 2 -> {
                if (inTopZone && inUEdge) yield 0;
                if (inBottomZone && inUEdge) yield 1;
                yield -1;
            }
            case 3 -> {
                if (inTopZone && u >= EDGE && u <= (1.0 - EDGE)) yield 0;
                if (inBottomZone && inLeftZone) yield 1;
                if (inBottomZone && inRightZone) yield 2;
                yield -1;
            }
            case 4 -> {
                if (inTopZone && inLeftZone) yield 0;
                if (inTopZone && inRightZone) yield 1;
                if (inBottomZone && inLeftZone) yield 2;
                if (inBottomZone && inRightZone) yield 3;
                yield -1;
            }
            default -> 0;
        };
    }
}