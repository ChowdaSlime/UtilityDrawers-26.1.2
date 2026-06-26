package net.drawers.utilitydrawers.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FramedDrawerItemModel implements ItemModel {

    private final ItemModel baseModel;

    private static final Field layersField;
    private static final Field activeLayerCountField;

    static {
        try {
            layersField = ItemStackRenderState.class.getDeclaredField("layers");
            layersField.setAccessible(true);
            activeLayerCountField = ItemStackRenderState.class.getDeclaredField("activeLayerCount");
            activeLayerCountField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("FramedDrawerItemModel: failed to reflect ItemStackRenderState fields", e);
        }
    }

    public FramedDrawerItemModel(ItemModel baseModel) {
        this.baseModel = baseModel;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int flags) {

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag identityTag = customData.copyTag();
            if (identityTag.contains("FrameState") || identityTag.contains("FaceState")) {
                state.appendModelIdentityElement(identityTag);
            }
        }

        baseModel.update(state, stack, resolver, context, level, owner, flags);

        if (!(stack.getItem() instanceof BlockItem)) return;
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        if (!tag.contains("FrameState") && !tag.contains("FaceState")) return;

        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

        BlockState frameState = null;
        if (tag.contains("FrameState")) {
            frameState = BlockState.CODEC.parse(ops, tag.get("FrameState")).result().orElse(null);
        }

        BlockState faceState = null;
        if (tag.contains("FaceState")) {
            faceState = BlockState.CODEC.parse(ops, tag.get("FaceState")).result().orElse(null);
        }

        if (frameState == null && faceState == null) return;

        try {
            ItemStackRenderState.LayerRenderState[] layers =
                    (ItemStackRenderState.LayerRenderState[]) layersField.get(state);
            int activeCount = (int) activeLayerCountField.get(state);

            final BlockState finalFrameState = frameState;
            final BlockState finalFaceState = faceState;

            for (int i = 0; i < activeCount; i++) {
                List<BakedQuad> quadList = layers[i].prepareQuadList();
                List<BakedQuad> originalQuads = List.copyOf(quadList);
                quadList.clear();

                for (BakedQuad quad : originalQuads) {
                    TextureAtlasSprite oldSprite = quad.materialInfo().sprite();
                    String path = oldSprite.contents().name().getPath();
                    Direction quadDir = quad.direction();
                    TextureAtlasSprite frameSprite = finalFrameState != null ? getSpriteFromBlockState(finalFrameState, quadDir) : null;
                    TextureAtlasSprite faceSprite = finalFaceState != null ? getSpriteFromBlockState(finalFaceState, quadDir) : null;

                    boolean isFrame = path.equals("block/framed_drawer_1")
                            || path.equals("block/framed_drawer_2")
                            || path.equals("block/framed_drawer_3")
                            || path.equals("block/framed_drawer_4");

                    boolean isFace = path.equals("block/framed_drawer_1_face")
                            || path.equals("block/framed_drawer_2_face")
                            || path.equals("block/framed_drawer_3_face")
                            || path.equals("block/framed_drawer_4_face");

                    if (frameSprite != null && isFrame) {
                        quadList.add(remapQuad(quad, oldSprite, frameSprite));
                    } else if (faceSprite != null && isFace) {
                        quadList.add(remapQuad(quad, oldSprite, faceSprite));
                    } else {
                        quadList.add(quad);
                    }
                }
            }
        } catch (IllegalAccessException e) {
        }
    }

    private static @Nullable TextureAtlasSprite getSpriteFromBlockState(BlockState state, Direction targetDir) {
        var blockModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        if (blockModel == null) return null;
        if (blockModel instanceof FramedDrawerBlockStateModel framed) {
            blockModel = framed.getOriginalModel();
        }

        var random = RandomSource.create(0L);
        List<BlockStateModelPart> parts = new ArrayList<>();
        blockModel.collectParts(random, parts);

        List<Direction> priority = new ArrayList<>();
        if (targetDir != null) {
            priority.add(targetDir);
        }
        for (Direction dir : Direction.values()) {
            if (dir != targetDir) priority.add(dir);
        }
        priority.add(null);

        for (Direction dir : priority) {
            for (var part : parts) {
                var quads = part.getQuads(dir);
                if (quads == null || quads.isEmpty()) continue;
                for (var quad : quads) {
                    var sprite = quad.materialInfo().sprite();
                    String path = sprite.contents().name().getPath();
                    if (path.contains("missingno") || path.contains("missing")) continue;
                    return sprite;
                }
            }
        }
        return null;
    }

    private BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite) {
        BakedQuad.MaterialInfo oldMat = quad.materialInfo();
        BakedQuad.MaterialInfo newMat = new BakedQuad.MaterialInfo(
                newSprite, oldMat.layer(), oldMat.itemRenderType(),
                oldMat.tintIndex(), oldMat.shade(), oldMat.lightEmission(), oldMat.ambientOcclusion()
        );

        return new BakedQuad(
                quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                calculateUVFromPosition(quad.position0(), quad.direction(), newSprite),
                calculateUVFromPosition(quad.position1(), quad.direction(), newSprite),
                calculateUVFromPosition(quad.position2(), quad.direction(), newSprite),
                calculateUVFromPosition(quad.position3(), quad.direction(), newSprite),
                quad.direction(), newMat, quad.bakedNormals(), quad.bakedColors()
        );
    }

    private long calculateUVFromPosition(org.joml.Vector3fc pos, Direction dir, TextureAtlasSprite sprite) {
        float x = pos.x();
        float y = pos.y();
        float z = pos.z();
        float u = 0.0f;
        float v = 0.0f;
        if (dir == null) dir = Direction.UP;
        switch (dir) {
            case UP -> { u = x; v = z; }
            case DOWN -> { u = x; v = 1.0f - z; }
            case NORTH -> { u = 1.0f - x; v = 1.0f - y; }
            case SOUTH -> { u = x; v = 1.0f - y; }
            case WEST -> { u = z; v = 1.0f - y; }
            case EAST -> { u = 1.0f - z; v = 1.0f - y; }
        }
        u = Math.max(0.0f, Math.min(1.0f, u));
        v = Math.max(0.0f, Math.min(1.0f, v));
        float uNew = sprite.getU(u);
        float vNew = sprite.getV(v);
        return (Float.floatToRawIntBits(vNew) & 0xFFFFFFFFL)
                | ((long) Float.floatToRawIntBits(uNew) << 32);
    }
}