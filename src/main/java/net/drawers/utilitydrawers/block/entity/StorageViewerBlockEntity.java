package net.drawers.utilitydrawers.block.entity;


import net.drawers.utilitydrawers.menu.StorageViewerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;


public class StorageViewerBlockEntity extends BlockEntity implements MenuProvider {


    @Override
    public Component getDisplayName() {
        return Component.translatable("container.utilitydrawers.storage_viewer");
    }


    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StorageViewerMenu(containerId, inventory,
                getStorageInterface(),
                this.getBlockPos());
    }




    private @Nullable BlockPos storageInterfacePos = null;


    public StorageViewerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_VIEWER_BLOCK_ENTITY.get(), pos, state);
    }


    public @Nullable BlockPos getStorageInterfacePos() {
        return storageInterfacePos;
    }


    public void setStorageInterfacePos(BlockPos pos) {
        this.storageInterfacePos = pos;
        setChanged();
    }


    public @Nullable StorageInterfaceBlockEntity getStorageInterface() {
        if (level == null || storageInterfacePos == null) return null;
        if (level.getBlockEntity(storageInterfacePos) instanceof StorageInterfaceBlockEntity sibe) {
            return sibe;
        }
        return null;
    }


    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (storageInterfacePos != null) {
            output.putLong("StorageInterfacePos", storageInterfacePos.asLong());
        }
    }


    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        long val = input.getLongOr("StorageInterfacePos", Long.MIN_VALUE);
        storageInterfacePos = (val != Long.MIN_VALUE) ? BlockPos.of(val) : null;
    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }


    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
