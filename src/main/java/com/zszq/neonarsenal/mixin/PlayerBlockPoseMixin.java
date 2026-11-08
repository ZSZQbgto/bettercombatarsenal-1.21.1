package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the blended blocking pose after the vanilla player pose has been computed. */
@Mixin(value = PlayerModel.class, priority = 100)
public abstract class PlayerBlockPoseMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void neonarsenal$blockPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        try {
            float progress = BlockPose.progress(player, Minecraft.getInstance().getFrameTimeNs() / 1_000_000_000.0F);
            if (progress > 0.0F) {
                BlockPose.apply((PlayerModel<?>) (Object) this, player, progress);
            } else {
                BlockPose.clearRenderBends((PlayerModel<?>) (Object) this);
            }
        } catch (Throwable ignored) {
            // Never break player rendering because of a missing/broken animation asset.
        }
    }
}
