package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class FluidDrawerRenderer implements BlockEntityRenderer<FluidDrawerBlockEntity, FluidDrawerRenderer.FluidDrawerRenderState> {

    private final Font font;

    private static final Identifier LOCK_TEXTURE =
            Identifier.fromNamespaceAndPath(UtilityDrawers.MODID, "textures/gui/lock.png");

    public FluidDrawerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    public static class FluidDrawerRenderState extends BlockEntityRenderState {
        public FluidStack[] fluids = new FluidStack[0];
        public long[] amounts = new long[0];
        public Direction facing = Direction.NORTH;
        public boolean locked;
        public net.minecraft.world.level.Level level;
        public net.minecraft.core.BlockPos pos;
    }

    @Override
    public FluidDrawerRenderState createRenderState() {
        return new FluidDrawerRenderState();
    }

    @Override
    public void extractRenderState(FluidDrawerBlockEntity blockEntity, FluidDrawerRenderState state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.locked = blockEntity.isLocked();
        state.level = blockEntity.getLevel();
        state.pos = blockEntity.getBlockPos();
        int slotCount = blockEntity.getSlotCount();

        if (state.fluids.length != slotCount) {
            state.fluids = new FluidStack[slotCount];
            state.amounts = new long[slotCount];
        }

        for (int i = 0; i < slotCount; i++) {
            FluidStack stack = blockEntity.getStoredFluid(i);
            state.fluids[i] = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
            state.amounts[i] = blockEntity.getStoredAmount(i);
        }
    }

    @Override
    public void submit(FluidDrawerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state.fluids.length == 0) return;

        poseStack.pushPose();

        try {
            poseStack.translate(0.5D, 0.5D, 0.5D);

            Direction facing = state.facing != null ? state.facing : Direction.NORTH;
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

            int slotCount = state.fluids.length;

            float[][] slotOffsets = switch (slotCount) {
                case 1 -> new float[][]{{0.0f, 0.0f, 0.435f}};
                case 2 -> new float[][]{{0.0f, 0.26f, 0.435f}, {0.0f, -0.18f, 0.435f}};
                case 3 -> new float[][]{{0.0f, 0.26f, 0.435f}, {-0.24f, -0.18f, 0.435f}, {0.24f, -0.18f, 0.435f}};
                case 4 -> new float[][]{{-0.24f, 0.26f, 0.435f}, {0.24f, 0.26f, 0.435f}, {-0.24f, -0.18f, 0.435f}, {0.24f, -0.18f, 0.435f}};
                default -> new float[][]{{0.0f, 0.0f, 0.435f}};
            };

            float[] fluidScales = switch (slotCount) {
                case 1 -> new float[]{0.6f};
                case 2 -> new float[]{0.25f, 0.25f};
                case 3 -> new float[]{0.25f, 0.25f, 0.25f};
                case 4 -> new float[]{0.25f, 0.25f, 0.25f, 0.25f};
                default -> new float[]{0.6f};
            };

            Minecraft mc = Minecraft.getInstance();

            for (int i = 0; i < slotCount; i++) {
                if (state.fluids[i] == null || state.fluids[i].isEmpty() || state.amounts[i] <= 0) continue;

                float ox = slotOffsets[i][0];
                float oy = slotOffsets[i][1];
                float oz = slotOffsets[i][2];

                float halfSize = switch (slotCount) {
                    case 1 -> 0.34f;
                    case 2 -> 0.22f;
                    default -> 0.20f;
                };

                FluidStack stack = state.fluids[i];

                net.minecraft.world.level.block.state.BlockState fluidState =
                        stack.getFluid().defaultFluidState().createLegacyBlock();

                TextureAtlasSprite sprite = mc.getModelManager()
                        .getBlockStateModelSet()
                        .get(fluidState)
                        .particleMaterial()
                        .sprite();

                int tint = -1;
                BlockColors blockColors = mc.getBlockColors();
                BlockTintSource source = blockColors.getTintSource(fluidState, 0);
                if (source != null && mc.level != null && state.pos != null) {
                    tint = source.colorInWorld(fluidState, mc.level, state.pos);
                }

                int fluidColor = tint == -1
                        ? (stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                           ? 0xFF3F76E4 : 0xFFFFFFFF)
                        : (0xFF000000 | tint);

                RenderType renderType = RenderTypes.entityCutout(sprite.atlasLocation());

                final float hs = halfSize;
                poseStack.pushPose();
                poseStack.translate(ox, oy, oz);

                submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                    var matrix = pose.pose();
                    consumer.addVertex(matrix, -hs, -hs, 0.0f).setColor(fluidColor).setUv(sprite.getU0(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix,  hs, -hs, 0.0f).setColor(fluidColor).setUv(sprite.getU1(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix,  hs,  hs, 0.0f).setColor(fluidColor).setUv(sprite.getU1(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix, -hs,  hs, 0.0f).setColor(fluidColor).setUv(sprite.getU0(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                });
                poseStack.popPose();

                poseStack.pushPose();
                float textOy = oy;
                if (slotCount == 1) {
                    textOy -= 0.1f;
                } else {
                    textOy -= 0.015f;
                }

                poseStack.translate(ox, textOy - (halfSize * 0.6f), 0.4376D);
                poseStack.scale(0.009f, -0.009f, 0.009f);

                String text = String.valueOf(state.amounts[i]);
                float textWidth = this.font.width(text);

                submitNodeCollector.submitText(
                        poseStack, -textWidth / 2.0f, 0.0f,
                        Component.literal(text).getVisualOrderText(),
                        false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
                poseStack.popPose();
            }

            if (state.locked) {
                poseStack.pushPose();
                float lockOx = 0.0f;
                float lockOy = 0.46875f;
                float lockOz = 0.501f;
                float lockScale = 0.0625f;

                poseStack.translate(lockOx, lockOy, lockOz);
                poseStack.scale(lockScale, lockScale, 1.0f);

                RenderType renderType = RenderTypes.entityCutout(LOCK_TEXTURE);

                submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                    var matrix = pose.pose();
                    consumer.addVertex(matrix, -0.5f, -0.5f, 0.0f).setColor(0xFFFFFFFF).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix,  0.5f, -0.5f, 0.0f).setColor(0xFFFFFFFF).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix,  0.5f,  0.5f, 0.0f).setColor(0xFFFFFFFF).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                    consumer.addVertex(matrix, -0.5f,  0.5f, 0.0f).setColor(0xFFFFFFFF).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0f, 0.0f, 1.0f);
                });

                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }
}