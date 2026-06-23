package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
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

    private static final Identifier WATER_STILL =
            Identifier.withDefaultNamespace("block/water_still");

    private static final float EDGE = 1.0f / 16.0f;
    private static final float TRIM = 1.0f / 16.0f;
    private static final float HALF_LOW = 0.5f - TRIM / 2.0f;
    private static final float HALF_HIGH = 0.5f + TRIM / 2.0f;

    public FluidDrawerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    public static class FluidDrawerRenderState extends BlockEntityRenderState {
        public FluidStack[] fluids = new FluidStack[0];
        public long[] amounts = new long[0];
        public long[] maxCapacities = new long[0];
        public Direction facing = Direction.NORTH;
        public boolean locked;
        public Level level;
        public BlockPos pos;
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
        if (state.maxCapacities.length != slotCount) {
            state.maxCapacities = new long[slotCount];
        }

        for (int i = 0; i < slotCount; i++) {
            FluidStack stack = blockEntity.getStoredFluid(i);
            state.fluids[i] = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
            state.amounts[i] = blockEntity.getStoredAmount(i);
            state.maxCapacities[i] = blockEntity.getMaxCapacity(i);
        }
    }

    private float[] getSlotUVBounds(int slot, int slotCount) {
        return switch (slotCount) {
            case 1 -> new float[]{ EDGE, 1f - EDGE, EDGE, 1f - EDGE };
            case 2 -> slot == 0 ? new float[]{ EDGE, 1f - EDGE, EDGE, HALF_LOW  }
                    : new float[]{ EDGE, 1f - EDGE, HALF_HIGH, 1f - EDGE };
            case 3 -> switch (slot) {
                case 0  -> new float[]{ EDGE, 1f - EDGE, EDGE,HALF_LOW  };
                case 1  -> new float[]{ EDGE, HALF_LOW,  HALF_HIGH, 1f - EDGE };
                default -> new float[]{ HALF_HIGH, 1f - EDGE, HALF_HIGH, 1f - EDGE };
            };
            case 4 -> new float[]{
                    (slot == 0 || slot == 2) ? EDGE : HALF_HIGH,
                    (slot == 0 || slot == 2) ? HALF_LOW : 1f - EDGE,
                    (slot == 0 || slot == 1) ? EDGE : HALF_HIGH,
                    (slot == 0 || slot == 1) ? HALF_LOW : 1f - EDGE,
            };
            default -> new float[]{ EDGE, 1f - EDGE, EDGE, 1f - EDGE };
        };
    }

    private String formatNumber(long count) {
        if (count >= 1_000_000) {
            String s = String.format(java.util.Locale.US, "%.2fM", count / 1_000_000.0f);
            return s.replace(".00M", "M");
        } else if (count >= 1_000) {
            String s = String.format(java.util.Locale.US, "%.1fk", count / 1_000.0f);
            return s.replace(".0k", "k");
        }
        return String.valueOf(count);
    }

    private static TextureAtlasSprite resolveSprite(FluidStack stack, Minecraft mc) {
        try {
            var fluidModel = mc.getModelManager()
                    .getFluidStateModelSet()
                    .get(stack.getFluid().defaultFluidState());
            return fluidModel.stillMaterial().sprite();
        }
        catch (Exception e) {
        }

        return mc.getAtlasManager().get(
                new SpriteId(TextureAtlas.LOCATION_BLOCKS, WATER_STILL));
    }

    private static int resolveColor(FluidStack stack) {
        var fluidModel = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(stack.getFluid().defaultFluidState());

        var tintSource = fluidModel.fluidTintSource();

        if (tintSource != null) {
            return tintSource.colorAsStack(stack);
        }

        return -1;
    }

    @Override
    public void submit(FluidDrawerRenderState state,
                       PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state.fluids.length == 0) return;

        poseStack.pushPose();
        try {
            poseStack.translate(0.5, 0.5, 0.5);

            Direction facing = state.facing != null ? state.facing : Direction.NORTH;
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

            int slotCount = state.fluids.length;
            final float oz = 0.435f;
            final float bgZ = 0.002f;
            final float fillZ = 0.003f;
            final float textZ = 0.440f;

            Minecraft mc = Minecraft.getInstance();

            for (int i = 0; i < slotCount; i++) {
                if (state.fluids[i] == null || state.fluids[i].isEmpty()) continue;

                float[] uvBounds = getSlotUVBounds(i, slotCount);
                float uMin = uvBounds[0], uMax = uvBounds[1];
                float vMin = uvBounds[2], vMax = uvBounds[3];

                float rLeft =  (uMin - 0.5f);
                float rRight =  (uMax - 0.5f);
                float rBottom  = -(vMax - 0.5f);
                float rTop = -(vMin - 0.5f);
                float rCenterX = (rLeft + rRight) / 2f;

                FluidStack stack = state.fluids[i];

                final TextureAtlasSprite sprite = resolveSprite(stack, mc);
                final int fluidColor = resolveColor(stack);

                float fillFraction = (state.maxCapacities[i] > 0 && state.amounts[i] > 0)
                        ? (float) state.amounts[i] / state.maxCapacities[i]
                        : 0f;

                var bgRenderType = RenderTypes.entityTranslucent(sprite.atlasLocation());

                poseStack.pushPose();
                poseStack.translate(0f, 0f, oz);

                submitNodeCollector.submitCustomGeometry(poseStack, bgRenderType, (pose, consumer) -> {
                    var matrix   = pose.pose();
                    int bgColor  = 0x88111111;
                    consumer.addVertex(matrix, rLeft,  rBottom, bgZ).setColor(bgColor).setUv(sprite.getU0(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                    consumer.addVertex(matrix, rRight, rBottom, bgZ).setColor(bgColor).setUv(sprite.getU1(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                    consumer.addVertex(matrix, rRight, rTop, bgZ).setColor(bgColor).setUv(sprite.getU1(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                    consumer.addVertex(matrix, rLeft,  rTop, bgZ).setColor(bgColor).setUv(sprite.getU0(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                });

                if (fillFraction > 0f) {
                    float slotHeight = rTop - rBottom;
                    float fillTopY = rBottom + slotHeight * fillFraction;
                    float vTop = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (1f - fillFraction);

                    submitNodeCollector.submitCustomGeometry(poseStack, bgRenderType, (pose, consumer) -> {
                        var matrix = pose.pose();
                        consumer.addVertex(matrix, rLeft,  rBottom,  fillZ).setColor(fluidColor).setUv(sprite.getU0(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                        consumer.addVertex(matrix, rRight, rBottom,  fillZ).setColor(fluidColor).setUv(sprite.getU1(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                        consumer.addVertex(matrix, rRight, fillTopY, fillZ).setColor(fluidColor).setUv(sprite.getU1(), vTop).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                        consumer.addVertex(matrix, rLeft,  fillTopY, fillZ).setColor(fluidColor).setUv(sprite.getU0(), vTop).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0f, 0f, 1f);
                    });
                }

                poseStack.popPose();

                if (state.amounts[i] > 0) {
                    poseStack.pushPose();
                    poseStack.translate(rCenterX, rBottom + 0.08f, textZ);
                    poseStack.scale(0.009f, -0.009f, 0.009f);

                    String text      = formatNumber(state.amounts[i]);
                    float  textWidth = this.font.width(text);
                    submitNodeCollector.submitText(
                            poseStack, -textWidth / 2.0f, 0.0f,
                            Component.literal(text).getVisualOrderText(),
                            false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0);
                    poseStack.popPose();
                }
            }

            if (state.locked) {
                poseStack.pushPose();
                poseStack.translate(0.0f, 0.46875f, 0.501f);
                poseStack.scale(0.0625f, 0.0625f, 1.0f);

                var lockRenderType = RenderTypes.entityCutout(LOCK_TEXTURE);
                submitNodeCollector.submitCustomGeometry(poseStack, lockRenderType, (pose, consumer) -> {
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