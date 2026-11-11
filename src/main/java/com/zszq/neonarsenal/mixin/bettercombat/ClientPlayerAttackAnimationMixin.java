package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.AttackAnimationTracker;
import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.logic.AnimatedHand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters Better Combat's {@code playAttackAnimation} for the local player while the fall-slam owns
 * the attack animation layer, and records every attack animation so the authored holding pose can
 * fade out while a swing is playing.
 *
 * <p>Only {@code bettercombat:two_handed_slam} started manually by
 * {@link com.zszq.neonarsenal.client.FallSlamAnimation#onExternalStart} may pass (exactly once, via
 * {@link FallSlamState#allowAnimation}). Other players' animations are never filtered. Priority 2000
 * applies this mixin after Better Combat's mixin, which is what adds the {@code playAttackAnimation}
 * method to {@code AbstractClientPlayer} in the first place.</p>
 */
@Mixin(targets = "net.minecraft.client.player.AbstractClientPlayer", remap = false, priority = 2000)
public class ClientPlayerAttackAnimationMixin {

    @Inject(method = "playAttackAnimation", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neonarsenal$filterFallSlamAnimation(String animation, AnimatedHand hand,
                                                     float length, float upswing, CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || player != localPlayer) {
            // Other players' animations always play; still record them for the holding pose.
            AttackAnimationTracker.onAnimationStart(player, length);
            return;
        }
        if (FallSlamState.allowAnimation) {
            FallSlamState.allowAnimation = false;
        } else if (com.zszq.neonarsenal.compat.ChargeThrustState.allowAnimation) {
            com.zszq.neonarsenal.compat.ChargeThrustState.allowAnimation = false;
        } else if (FallSlamState.waitingForSlam
                || com.zszq.neonarsenal.compat.ChargeThrustState.blockAttacks) {
            ci.cancel();
            return;
        }
        AttackAnimationTracker.onAnimationStart(player, length);
    }

    @Inject(method = "stopAttackAnimation", at = @At("HEAD"), remap = false, require = 0)
    private void neonarsenal$trackAnimationStop(float length, CallbackInfo ci) {
        AttackAnimationTracker.onAnimationStop((AbstractClientPlayer) (Object) this);
    }
}
