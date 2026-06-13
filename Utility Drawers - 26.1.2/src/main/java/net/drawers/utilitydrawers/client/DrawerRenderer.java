package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.Nullable;
@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class DrawerRenderer implements BlockEntityRenderer<DrawerBlockEntity, DrawerRenderer.DrawerRenderState> {

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public DrawerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    public static class DrawerRenderState extends BlockEntityRenderState {
        public ItemStackRenderState[] itemStates = new ItemStackRenderState[0];
        public long[] counts = new long[0];
        public Direction facing = Direction.NORTH;
    }

    @Override
    public DrawerRenderState createRenderState() {
        return new DrawerRenderState();
    }

    @Override
    public void extractRenderState(DrawerBlockEntity blockEntity, DrawerRenderState state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int slotCount = blockEntity.getSlotCount();

        if (state.itemStates.length != slotCount) {
            state.itemStates = new ItemStackRenderState[slotCount];
            state.counts = new long[slotCount];
            for (int i = 0; i < slotCount; i++) {
                state.itemStates[i] = new ItemStackRenderState();
            }
        }

        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = blockEntity.getStoredItem(i);
            state.counts[i] = blockEntity.getStoredCount(i);
            if (!stack.isEmpty()) {
                this.itemModelResolver.updateForTopItem(state.itemStates[i], stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
            } else {
                state.itemStates[i].clear();
            }
        }
    }

    @Override
    public void submit(DrawerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state.itemStates.length == 0 || state.itemStates[0].isEmpty()) return;

        poseStack.pushPose();

        try {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            Direction facing = state.facing != null ? state.facing : Direction.NORTH;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, 0.35D);
            poseStack.scale(0.75f, 0.75f, 0.75f);
            state.itemStates[0].submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            if (state.counts != null && state.counts.length > 0 && state.counts[0] > 0) {
                poseStack.pushPose();
                poseStack.translate(0.0D, -0.3D, 0.501D);
                poseStack.scale(0.0125f, -0.0125f, 0.0125f);
                String text = String.valueOf(state.counts[0]);
                float textWidth = this.font.width(text);
                submitNodeCollector.submitText(
                        poseStack, -textWidth / 2.0f, 0.0f, Component.literal(text).getVisualOrderText(),
                        false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }
}