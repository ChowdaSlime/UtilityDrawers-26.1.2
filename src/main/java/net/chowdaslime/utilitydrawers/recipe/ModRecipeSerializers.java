package net.chowdaslime.utilitydrawers.recipe;

import com.mojang.serialization.MapCodec;
import net.chowdaslime.utilitydrawers.UtilityDrawers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, UtilityDrawers.MODID);

    public static final MapCodec<ResetUpgradeRecipe> RESET_UPGRADE_CODEC =
            MapCodec.unit(ResetUpgradeRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ResetUpgradeRecipe> RESET_UPGRADE_STREAM_CODEC =
            StreamCodec.unit(new ResetUpgradeRecipe());

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResetUpgradeRecipe>> RESET_UPGRADE =
            RECIPE_SERIALIZERS.register("reset_upgrade",
                    () -> new RecipeSerializer<>(RESET_UPGRADE_CODEC, RESET_UPGRADE_STREAM_CODEC));
}