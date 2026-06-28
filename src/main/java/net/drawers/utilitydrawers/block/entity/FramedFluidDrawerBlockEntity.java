package net.drawers.utilitydrawers.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

public class FramedFluidDrawerBlockEntity extends FluidDrawerBlockEntity implements IFramedBlockEntity {

    private BlockState frameState = null;
    private BlockState faceState = null;
    public static final ModelProperty<BlockState> FRAME_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<BlockState> FACE_PROPERTY = new ModelProperty<>();

    public FramedFluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRAMED_FLUID_DRAWER_BLOCK_ENTITY.get(), pos, state);
    }

    public BlockState getFrameState() { return frameState; }
    public BlockState getFaceState()  { return faceState; }

    public void setFrameState(BlockState state) {
        this.frameState = state;
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void setFaceState(BlockState state) {
        this.faceState = state;
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(FRAME_PROPERTY, this.frameState)
                .with(FACE_PROPERTY, this.faceState)
                .build();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (frameState != null)
            output.store("FrameState", BlockState.CODEC, frameState);
        if (faceState != null)
            output.store("FaceState", BlockState.CODEC, faceState);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        frameState = input.read("FrameState", BlockState.CODEC).orElse(null);
        faceState  = input.read("FaceState",  BlockState.CODEC).orElse(null);
    }

    @Override
    public CompoundTag saveDrawerData(HolderLookup.Provider provider) {
        CompoundTag tag = super.saveDrawerData(provider);
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        if (connectedInterface != null) {
            tag.putLong("ConnectedInterface", connectedInterface.asLong());
        }
        if (frameState != null)
            tag.put("FrameState", BlockState.CODEC.encodeStart(ops, frameState).getOrThrow());
        if (faceState != null)
            tag.put("FaceState", BlockState.CODEC.encodeStart(ops, faceState).getOrThrow());
        return tag;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveDrawerData(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        BlockPos savedInterface = this.connectedInterface;
        super.onDataPacket(connection, input);
        frameState = input.read("FrameState", BlockState.CODEC).orElse(null);
        faceState  = input.read("FaceState",  BlockState.CODEC).orElse(null);
        if (this.level != null && this.level.isClientSide()) {
            this.connectedInterface = savedInterface;
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void loadContentsFromTag(CompoundTag tag) {
        super.loadContentsFromTag(tag);
        if (level != null) {
            var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            frameState = tag.contains("FrameState")
                    ? BlockState.CODEC.parse(ops, tag.get("FrameState")).resultOrPartial().orElse(null)
                    : null;
            faceState = tag.contains("FaceState")
                    ? BlockState.CODEC.parse(ops, tag.get("FaceState")).resultOrPartial().orElse(null)
                    : null;
        }
    }
}