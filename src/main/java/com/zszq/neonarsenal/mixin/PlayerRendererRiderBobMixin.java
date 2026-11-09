package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.RiderBob;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies {@link RiderBob} in front of the player model, so the whole rider (body, held items,
 * layers) heaves with the mount's gait.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererRiderBobMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void neonarsenal$applyRiderBob(AbstractClientPlayer entity, float entityYaw, float partialTicks,
                                           PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                           CallbackInfo ci) {
        try {
            RiderBob.apply(entity, partialTicks, poseStack);
        } catch (Throwable ignored) {
            // Never break rendering because of a decorative bob.
        }
    }
}
