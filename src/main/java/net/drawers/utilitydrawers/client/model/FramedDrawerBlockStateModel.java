package net.drawers.utilitydrawers.client.model;

import net.drawers.utilitydrawers.block.entity.FramedDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FramedDrawerBlockStateModel implements BlockStateModel {

    private final BlockStateModel originalModel;

    public FramedDrawerBlockStateModel(BlockStateModel originalModel) {
        this.originalModel = originalModel;
    }

    public BlockStateModel getOriginalModel() {
        return originalModel;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        originalModel.collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return originalModel.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return originalModel.materialFlags();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        ModelData data = level.getModelData(pos);

        BlockState frameState = data.get(FramedDrawerBlockEntity.FRAME_PROPERTY);
        BlockState faceState = data.get(FramedDrawerBlockEntity.FACE_PROPERTY);

        if (frameState == null && faceState == null) {
            originalModel.collectParts(level, pos, state, random, parts);
            return;
        }

        List<BlockStateModelPart> originalParts = new ArrayList<>();
        originalModel.collectParts(level, pos, state, random, originalParts);

        for (BlockStateModelPart part : originalParts) {
            parts.add(wrapPart(part, frameState, faceState));
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

    private BlockStateModelPart wrapPart(BlockStateModelPart original, @Nullable BlockState frameState, @Nullable BlockState faceState) {
        return new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction dir) {
                List<BakedQuad> originalQuads = original.getQuads(dir);
                if (originalQuads == null || originalQuads.isEmpty()) return originalQuads;

                List<BakedQuad> newQuads = new ArrayList<>();
                for (BakedQuad quad : originalQuads) {
                    TextureAtlasSprite oldSprite = quad.materialInfo().sprite();
                    String texturePath = oldSprite.contents().name().getPath();
                    Direction quadDir = quad.direction();

                    TextureAtlasSprite frameSprite = frameState != null ? getSpriteFromBlockState(frameState, quadDir) : null;
                    TextureAtlasSprite faceSprite = faceState != null ? getSpriteFromBlockState(faceState, quadDir) : null;

                    boolean isFrame = texturePath.equals("block/framed_drawer_1")
                            || texturePath.equals("block/framed_drawer_2")
                            || texturePath.equals("block/framed_drawer_3")
                            || texturePath.equals("block/framed_drawer_4");

                    boolean isFace = texturePath.equals("block/framed_drawer_1_face")
                            || texturePath.equals("block/framed_drawer_2_face")
                            || texturePath.equals("block/framed_drawer_3_face")
                            || texturePath.equals("block/framed_drawer_4_face");

                    if (frameSprite != null && isFrame) {
                        newQuads.add(remapQuad(quad, oldSprite, frameSprite));
                    } else if (faceSprite != null && isFace) {
                        newQuads.add(remapQuad(quad, oldSprite, faceSprite));
                    } else {
                        newQuads.add(quad);
                    }
                }
                return newQuads;
            }

            @Override
            public boolean useAmbientOcclusion() {
                return original.useAmbientOcclusion();
            }

            @Override
            public Material.Baked particleMaterial() {
                return original.particleMaterial();
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags() {
                return original.materialFlags();
            }
        };
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