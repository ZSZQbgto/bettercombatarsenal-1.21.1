package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Folds the elytra wings into the vanilla sneaking pose while blocking.
 *
 * <p>The blocking torso is leaned forward, so the normal spread wings would cut through it. Forcing
 * the crouching elytra pose makes the wings tuck against the back; the layer mixin then keeps that
 * pose attached to the posed torso.</p>
 */
@Mixin(ElytraModel.class)
public abstract class ElytraModelBlockPoseMixin {

    @Redirect(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isCrouching()Z"))
    private boolean neonarsenal$useSneakWingPoseWhileBlocking(LivingEntity entity) {
        return entity.isCrouching()
                || (entity instanceof AbstractClientPlayer player && BlockPose.isBlocking(player));
    }
}
