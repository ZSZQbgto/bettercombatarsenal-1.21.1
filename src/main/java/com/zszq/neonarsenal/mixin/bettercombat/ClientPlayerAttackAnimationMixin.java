package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.logic.AnimatedHand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters Better Combat's {@code playAttackAnimation} for the local player while the fall-slam owns
 * the attack animation layer.
 *
 * <p>Only {@code bettercombat:two_handed_slam} started manually by
 * {@link com.zszq.neonarsenal.client.FallSlamAnimation#onExternalStart} may pass (exactly once, via
 * {@link FallSlamState#allowAnimation}). Other players' animations are never filtered. Priority 2000
 * applies this mixin after Better Combat's mixin, which is what adds the {@code playAttackAnimation}
 * method to {@code AbstractClientPlayer} in the first place.</p>
 */
@Mixin(targets = "net.minecraft.client.player.AbstractClientPlayer", remap = false, priority = 2000)
public class ClientPlayerAttackAnimationMixin {

    @Inject(method = "playAttackAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void neonarsenal$filterFallSlamAnimation(String animation, AnimatedHand hand,
                                                     float length, float upswing, CallbackInfo ci) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || (Object) this != localPlayer) {
            return;
        }
        if (FallSlamState.allowAnimation) {
            FallSlamState.allowAnimation = false;
            return;
        }
        if (FallSlamState.waitingForSlam) {
            ci.cancel();
        }
    }
}
