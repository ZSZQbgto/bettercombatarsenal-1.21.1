package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the elytra attached to the blocking torso.
 *
 * <p>The vanilla elytra layer renders its wings at the player model origin and never follows the
 * {@code body} part, so the custom blocking pose (which rotates and moves the torso) made the wings
 * float away from the shoulders. While the block pose is active, this mixin applies the current body
 * part transform to the elytra pose stack, so the wings follow the torso exactly like the arms and
 * head do.</p>
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerBlockPoseMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void neonarsenal$attachElytraToBody(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                float partialTicks, float ageInTicks, float netHeadYaw,
                                                float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!((Object) this instanceof ElytraLayer<?, ?> layer)) {
            return;
        }
        if (!(layer.getParentModel() instanceof PlayerModel<?> model)) {
            return;
        }
        if (BlockPose.progressOf(player) <= 0.001F) {
            return;
        }
        // The elytra wing root sits at the vanilla body pivot, so the body transform places the
        // wings on the shoulders and keeps them there while the torso is posed.
        model.body.translateAndRotate(poseStack);
    }
}
