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
 * Priority-0 variant of {@link CapeLayerVanillaMixin}: runs first if the Mixin version applies
 * lower-priority callbacks first. Whichever priority variant runs first wins; all of them only set
 * the same suppression flag, so the extra variants are harmless when several run.
 */
@Mixin(value = CapeLayer.class, priority = 0)
public abstract class CapeLayerVanillaEarlyMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void neonarsenal$suppressPalCapeEarly(PoseStack poseStack, MultiBufferSource buffer,
                                                  int packedLight, AbstractClientPlayer player,
                                                  float limbSwing, float limbSwingAmount,
                                                  float partialTicks, float ageInTicks,
                                                  float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.beginCapeRender();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neonarsenal$restorePalCapeEarly(PoseStack poseStack, MultiBufferSource buffer,
                                                 int packedLight, AbstractClientPlayer player,
                                                 float limbSwing, float limbSwingAmount,
                                                 float partialTicks, float ageInTicks,
                                                 float netHeadYaw, float headPitch, CallbackInfo ci) {
        CapeRenderState.endCapeRender();
    }
}
