package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.ThirdPersonAttackPose;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Exposes the player currently being rendered to the item renderer.
 *
 * <p>Vanilla's {@code BlockEntityWithoutLevelRenderer} does not receive the entity, so the
 * third-person attack item pose needs the entity the held-item layer is drawing. This mixin sets it
 * for the duration of {@link ItemInHandLayer#render} (all players, not just the local one).</p>
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerContextMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void neonarsenal$beginEntityItems(PoseStack poseStack, MultiBufferSource buffer,
                                              int packedLight, LivingEntity entity,
                                              float limbSwing, float limbSwingAmount,
                                              float partialTicks, float ageInTicks,
                                              float netHeadYaw, float headPitch, CallbackInfo ci) {
        ThirdPersonAttackPose.beginEntity(entity);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neonarsenal$endEntityItems(PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, LivingEntity entity,
                                            float limbSwing, float limbSwingAmount,
                                            float partialTicks, float ageInTicks,
                                            float netHeadYaw, float headPitch, CallbackInfo ci) {
        ThirdPersonAttackPose.endEntity();
    }
}
