package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.BlockPose;
import com.zszq.neonarsenal.client.HoldPose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the blended blocking pose after the vanilla player pose has been computed, and the
 * authored main-hand holding pose while no block pose is active.
 */
@Mixin(value = PlayerModel.class, priority = 100)
public abstract class PlayerBlockPoseMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void neonarsenal$blockPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        try {
            PlayerModel<?> model = (PlayerModel<?>) (Object) this;
            float progress = BlockPose.progress(player, Minecraft.getInstance().getFrameTimeNs() / 1_000_000_000.0F);
            if (progress > 0.0F) {
                BlockPose.apply(model, player, progress);
                // The block pose owns the arms; let the holding pose fade out meanwhile.
                HoldPose.decay(player);
                HoldPose.clear(model);
            } else {
                BlockPose.clearRenderBends(model);
                HoldPose.apply(model, player, limbSwing, limbSwingAmount);
            }
        } catch (Throwable ignored) {
            // Never break player rendering because of a missing/broken animation asset.
        }
    }
}
