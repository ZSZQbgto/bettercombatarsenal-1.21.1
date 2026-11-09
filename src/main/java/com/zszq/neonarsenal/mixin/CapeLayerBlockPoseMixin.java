package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the vanilla cape attached to the blocking torso.
 *
 * <p>Like the elytra, the vanilla cape layer renders {@code PlayerModel#cloak} at the player model
 * origin instead of following the {@code body} part, so the custom blocking pose (which rotates and
 * moves the torso forward) left the cape hanging at its old position. While the block pose is
 * active this mixin pushes the current body transform onto the layer pose stack, so the cape hangs
 * from the posed shoulders and follows the torso through the 0.1 s enter/exit blend. The transform
 * is popped again when the layer returns, so layers rendered after it are not affected.</p>
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerBlockPoseMixin {

    /** {@return whether this layer currently has the body transform on the shared pose stack}. */
    @Unique
    private boolean neonarsenal$capePoseApplied;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"))
    private void neonarsenal$attachCapeToBody(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                              AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                              float partialTicks, float ageInTicks, float netHeadYaw,
                                              float headPitch, CallbackInfo ci) {
        neonarsenal$capePoseApplied = false;
        if (BlockPose.progressOf(player) <= 0.001F) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((CapeLayer) (Object) this).getParentModel();
        if (model == null) {
            return;
        }
        // The cloak pivot sits at the vanilla body pivot, so the body transform places the cape on
        // the posed shoulders and keeps it there while the torso leans in.
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        neonarsenal$capePoseApplied = true;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("RETURN"))
    private void neonarsenal$restoreCapePose(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                             AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                             float partialTicks, float ageInTicks, float netHeadYaw,
                                             float headPitch, CallbackInfo ci) {
        if (neonarsenal$capePoseApplied) {
            neonarsenal$capePoseApplied = false;
            poseStack.popPose();
        }
    }
}
