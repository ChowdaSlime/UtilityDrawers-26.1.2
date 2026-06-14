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
        if (state.itemStates.length == 0) return;

        poseStack.pushPose();

        try {
            poseStack.translate(0.5D, 0.5D, 0.5D);

            Direction facing = state.facing != null ? state.facing : Direction.NORTH;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));

            int slotCount = state.itemStates.length;

            float[][] slotOffsets = switch (slotCount) {
                case 1 -> new float[][] {
                        {0.0f, 0.0f}           // single slot
                };
                case 2 -> new float[][] {
                        {0.0f, 0.28f},         // slot 0
                        {0.0f, -0.18f}         // slot 1
                };
                case 3 -> new float[][] {
                        {0.0f, 0.28f},         // slot 0
                        {-0.24f, -0.21f},      // slot 1
                        {0.24f, -0.21f}       // slot 2
                };
                case 4 -> new float[][] {
                        {-0.24f, 0.28f},       // slot 0
                        {0.24f, 0.28f},        // slot 1
                        {-0.24f, -0.21f},      // slot 2
                        {0.24f, -0.21f}        // slot 3
                };
                default -> new float[][] {{0.0f, 0.0f}};
            };

            // Scale of each item render
            float[] itemScales = switch (slotCount) {
                case 1 -> new float[] {0.75f};
                case 2 -> new float[] {0.55f, 0.55f};
                case 3 -> new float[] {0.45f, 0.4f, 0.4f};
                case 4 -> new float[] {0.4f, 0.4f, 0.4f, 0.4f};
                default -> new float[] {0.75f};
            };

            for (int i = 0; i < slotCount; i++) {
                if (state.itemStates[i] == null || state.itemStates[i].isEmpty()) continue;

                float ox = slotOffsets[i][0]; // horizontal position on face
                float oy = slotOffsets[i][1]; // vertical position on face
                float scale = itemScales[i];  // item render size

                // Render the item model
                // translate Z (0.35D) pushes item out toward the front face surface
                poseStack.pushPose();
                poseStack.translate(ox, oy, 0.35D);
                poseStack.scale(scale, scale, scale);
                state.itemStates[i].submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();

                // Render the count text below the item
                // Z (0.501D) pushes text slightly in front of the face to avoid z-fighting
                // scale Y is negative to flip text right-side up (block space is Y-flipped vs screen space)
                // 0.009f scale converts from font pixel units to block units
                if (state.counts != null && state.counts.length > i && state.counts[i] > 0) {
                    poseStack.pushPose();
                    poseStack.translate(ox, oy - (scale * 0.3f), 0.501D); // (scale * 0.55f) offsets text below item center
                    poseStack.scale(0.009f, -0.009f, 0.009f);
                    String text = String.valueOf(state.counts[i]);
                    float textWidth = this.font.width(text); // measure text width for centering
                    submitNodeCollector.submitText(
                            poseStack, -textWidth / 2.0f, 0.0f, // negative half-width centers text horizontally
                            Component.literal(text).getVisualOrderText(),
                            false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
                    poseStack.popPose();
                }
            }
        } finally {
            poseStack.popPose();
        }
    }
}