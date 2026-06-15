package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.world.phys.AABB;
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
                // CHANGED: Switched from GUI to FIXED so blocks render flat and reliably in physical space.
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
                case 1 -> new float[][]{{0.0f, 0.0f, 0.4f}};
                case 2 -> new float[][]{{0.0f, 0.26f, 0.4f}, {0.0f, -0.18f, 0.4f}};
                case 3 -> new float[][]{{0.0f, 0.26f, 0.4f}, {-0.24f, -0.18f, 0.4f}, {0.24f, -0.18f, 0.4f}};
                case 4 -> new float[][]{{-0.24f, 0.26f, 0.4f}, {0.24f, 0.26f, 0.4f}, {-0.24f, -0.18f, 0.4f}, {0.24f, -0.18f, 0.4f}};
                default -> new float[][]{{0.0f, 0.0f, 0.4f}};
            };

            float[] itemScales = switch (slotCount) {
                case 1 -> new float[]{0.75f};
                case 2 -> new float[]{0.45f, 0.45f};
                case 3 -> new float[]{0.45f, 0.45f, 0.45f};
                case 4 -> new float[]{0.45f, 0.45f, 0.45f, 0.45f};
                default -> new float[]{0.75f};
            };


            for (int i = 0; i < slotCount; i++) {
                if (state.itemStates[i] == null || state.itemStates[i].isEmpty()) continue;

                float ox = slotOffsets[i][0];
                float oy = slotOffsets[i][1];
                float oz = slotOffsets[i][2];
                float baseScale = itemScales[i];
                float itemScale = baseScale;
                boolean isFlatItem = false;

                AABB box = state.itemStates[i].getModelBoundingBox();
                if (box != null) {
                    double zThickness = box.maxZ - box.minZ;
                    if (zThickness < 0.25) {
                        isFlatItem = true;
                        oz += 0.05f;

                        switch (slotCount) {
                            case 1 -> itemScale *= 0.70f;
                            case 2 -> itemScale *= 0.65f;
                            case 3 -> itemScale *= 0.65f;
                            case 4 -> itemScale *= 0.65f;
                        }
                    }
                    float itemOx = ox;
                    if (isFlatItem && slotCount == 1) {
                        itemOx -= 0.015f;
                    } if (isFlatItem && slotCount == 2) {
                        itemOx -= 0.015f;
                    }

                    poseStack.pushPose();
                    poseStack.translate(itemOx, oy, oz);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                    poseStack.scale(itemScale, itemScale, itemScale);

                    if (box != null) {
                        double offsetX = -(box.maxX + box.minX) / 2.0;
                        double offsetY = -(box.maxY + box.minY) / 2.0;
                        double offsetZ = -(box.maxZ + box.minZ) / 2.0;
                        poseStack.translate(offsetX, offsetY, offsetZ);
                    }

                    state.itemStates[i].submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                    poseStack.popPose();

                    if (state.counts != null && state.counts.length > i && state.counts[i] > 0) {
                        poseStack.pushPose();
                        float textOx = ox;
                        if (isFlatItem) {
                            if (ox < 0.0f) {
                                textOx += 0.005f;
                            } else if (ox > 0.0f) {
                                textOx -= 0.005f;
                            }
                        } else {
                            if (ox < 0.0f) {
                                textOx -= 0.01f;
                            } else if (ox > 0.0f) {
                                textOx += 0.01f;
                            }
                        }
                        float textOy = oy;
                        if (slotCount == 1) {
                            textOy -= 0.1f;
                        } if (slotCount != 1) {
                            textOy -= 0.015f;
                        }

                        poseStack.translate(textOx, textOy - (baseScale * 0.3f), 0.4376D);
                        poseStack.scale(0.009f, -0.009f, 0.009f);
                        String text = String.valueOf(state.counts[i]);
                        float textWidth = this.font.width(text);
                        submitNodeCollector.submitText(
                                poseStack, -textWidth / 2.0f, 0.0f,
                                Component.literal(text).getVisualOrderText(),
                                false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
                        poseStack.popPose();
                    }
                }
            }
        } finally {
            poseStack.popPose();
        }
    }
}