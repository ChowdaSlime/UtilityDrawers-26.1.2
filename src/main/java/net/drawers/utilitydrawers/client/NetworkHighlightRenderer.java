package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class NetworkHighlightRenderer {

    @SubscribeEvent
    public static void renderNetworkHighlights(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack remoteStack = ItemStack.EMPTY;

        if (mainHand.getItem() instanceof StorageRemoteItem) remoteStack = mainHand;
        else if (offHand.getItem() instanceof StorageRemoteItem) remoteStack = offHand;

        if (remoteStack.isEmpty()) return;
        if (!StorageRemoteItem.isLinkMode(remoteStack)) return;

        BlockPos boundPos = StorageRemoteItem.getBoundInterface(remoteStack);
        if (boundPos == null) return;

        if (!(mc.level.getBlockEntity(boundPos) instanceof StorageInterfaceBlockEntity interfaceEntity)) return;

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) return;

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());

        int color = ARGB.colorFromFloat(1.0F, 0.0F, 0.5F, 1.0F);

        VoxelShape blockShape = Shapes.block();

        renderOutline(poseStack, consumer, blockShape, boundPos, cameraPos, color);

        for (BlockPos drawerPos : interfaceEntity.getConnectedDrawers()) {
            if (mc.level.getBlockEntity(drawerPos) instanceof DrawerBlockEntity) {
                renderOutline(poseStack, consumer, blockShape, drawerPos, cameraPos, color);
            }
        }
    }

    private static void renderOutline(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape,
                                      BlockPos pos, Vec3 cameraPos, int color) {
        ShapeRenderer.renderShape(
                poseStack,
                consumer,
                shape,
                pos.getX() - cameraPos.x,
                pos.getY() - cameraPos.y,
                pos.getZ() - cameraPos.z,
                color,
                4.0F
        );
    }
}