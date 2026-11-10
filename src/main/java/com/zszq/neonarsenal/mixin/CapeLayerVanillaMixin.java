package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.fx.CapeRenderState;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts the vanilla-cape window at the head of {@code CapeLayer.render} (priority 500).
 *
 * <p>Several priority variants of this mixin exist so that at least one runs before Player
 * Animator Library's {@code CapeLayerMixin} (default priority 1000) regardless of the callback
 * ordering used by the running Mixin version.</p>
 */
@Mixin(value = CapeLayer.class, priority = 500)
public abstract class CapeLayerVanillaMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void neonarsenal$suppressPalCape(PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight, AbstractClientPlayer player,
                                             float limbSwing, float limbSwingAmount,
                                             float partialTicks, float ageInTicks,
                                             float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.beginCapeRender();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neonarsenal$restorePalCape(PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, AbstractClientPlayer player,
                                            float limbSwing, float limbSwingAmount,
                                            float partialTicks, float ageInTicks,
                                            float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.endCapeRender();
    }
}
