package net.drawers.utilitydrawers.data;

import com.mojang.serialization.Codec;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, UtilityDrawers.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> REMOTE_MODE =
            DATA_COMPONENTS.register("remote_mode",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> BOUND_INTERFACE =
            DATA_COMPONENTS.register("bound_interface",
                    () -> DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECT_MODE =
            DATA_COMPONENTS.register("select_mode",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> MULTI_SELECT_CORNER =
            DATA_COMPONENTS.register("multi_select_corner",
                    () -> DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WirelessNetworkKey>> WIRELESS_NETWORK_KEY =
            DATA_COMPONENTS.register("wireless_network_key",
                    () -> DataComponentType.<WirelessNetworkKey>builder()
                            .persistent(WirelessNetworkKey.CODEC)
                            .networkSynchronized(WirelessNetworkKey.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HAS_COPIED_SETTINGS =
            DATA_COMPONENTS.register("has_copied_settings",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}