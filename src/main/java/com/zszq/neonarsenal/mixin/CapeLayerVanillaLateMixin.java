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
 * Priority-2000 variant of {@link CapeLayerVanillaMixin}: runs first if the Mixin version applies
 * higher-priority callbacks first. Whichever priority variant runs first wins; all of them only set
 * the same suppression flag, so the extra variants are harmless when several run.
 */
@Mixin(value = CapeLayer.class, priority = 2000)
public abstract class CapeLayerVanillaLateMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void neonarsenal$suppressPalCapeLate(PoseStack poseStack, MultiBufferSource buffer,
                                                 int packedLight, AbstractClientPlayer player,
                                                 float limbSwing, float limbSwingAmount,
                                                 float partialTicks, float ageInTicks,
                                                 float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.beginCapeRender();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neonarsenal$restorePalCapeLate(PoseStack poseStack, MultiBufferSource buffer,
                                                int packedLight, AbstractClientPlayer player,
                                                float limbSwing, float limbSwingAmount,
                                                float partialTicks, float ageInTicks,
                                                float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.endCapeRender();
    }
}
