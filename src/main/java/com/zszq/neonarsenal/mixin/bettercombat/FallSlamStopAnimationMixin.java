package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.client.FallSlamAnimation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Protects the manually played fall-slam animation from Better Combat's own fade/cancel path.
 *
 * <p>When the server disables Better Combat attacks, its client tick (or an item switch) can call
 * {@code cancelWeaponSwing()}, which calls {@code stopAttackAnimation}. That would kill the windup
 * pose mid-fall. While the fall-slam animation owns the layer only
 * {@link FallSlamAnimation} itself may stop it (its own calls set the allow flag).</p>
 *
 * <p>This mixin lives on {@code AbstractClientPlayer} and Better Combat's networked stop packet also
 * stops <b>other</b> players' animations, so the local-player check is required: only the local
 * player's manual windup is protected.</p>
 */
@Mixin(value = AbstractClientPlayer.class, priority = 2000, remap = false)
public class FallSlamStopAnimationMixin {

    @Inject(method = "stopAttackAnimation", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neonarsenal$protectWindup(float length, CallbackInfo ci) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if ((Object) this != localPlayer) {
            return;
        }
        if (FallSlamAnimation.isControlling() && !FallSlamAnimation.isAllowingStop()) {
            ci.cancel();
            return;
        }
        if (com.zszq.neonarsenal.client.ChargeThrustAnimation.isControlling()
                && !com.zszq.neonarsenal.client.ChargeThrustAnimation.isAllowingStop()) {
            ci.cancel();
        }
    }
}
