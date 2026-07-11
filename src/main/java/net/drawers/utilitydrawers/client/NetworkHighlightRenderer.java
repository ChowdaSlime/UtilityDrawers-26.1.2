package net.drawers.utilitydrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.drawers.utilitydrawers.UtilityDrawers;
import net.drawers.utilitydrawers.block.entity.CompactingDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.DrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.FluidDrawerBlockEntity;
import net.drawers.utilitydrawers.block.entity.StorageInterfaceBlockEntity;
import net.drawers.utilitydrawers.item.StorageRemoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = UtilityDrawers.MODID, value = Dist.CLIENT)
public class NetworkHighlightRenderer {

    private static final double MAX_DISTANCE_SQ = 64 * 64;

    @SubscribeEvent
    public static void renderNetworkHighlights(SubmitCustomGeometryEvent event) {
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

        LevelRenderState levelRenderState = event.getLevelRenderState();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        int color = ARGB.colorFromFloat(1.0F, 0.0F, 0.5F, 1.0F);
        VoxelShape blockShape = Shapes.block();

        if (Vec3.atCenterOf(boundPos).distanceToSqr(player.position()) <= MAX_DISTANCE_SQ) {
            renderOutline(poseStack, collector, blockShape, boundPos, cameraPos, color);
        }

        for (BlockPos drawerPos : interfaceEntity.getConnectedDrawers()) {
            if (Vec3.atCenterOf(drawerPos).distanceToSqr(player.position()) > MAX_DISTANCE_SQ) continue;

            BlockEntity be = mc.level.getBlockEntity(drawerPos);
            if (be instanceof DrawerBlockEntity ||
                    be instanceof FluidDrawerBlockEntity ||
                    be instanceof CompactingDrawerBlockEntity) {
                renderOutline(poseStack, collector, blockShape, drawerPos, cameraPos, color);
            }
        }
    }

    private static void renderOutline(PoseStack poseStack, SubmitNodeCollector collector, VoxelShape shape,
                                      BlockPos pos, Vec3 cameraPos, int color) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
        collector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), color, 4.0F, false);
        poseStack.popPose();
    }
}