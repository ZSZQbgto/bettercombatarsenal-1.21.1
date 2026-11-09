package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.compat.ChargeThrustState;
import com.zszq.neonarsenal.compat.FallSlamState;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks the held-attack path ({@code continueAttack}) while the fall-slam windup holds attacks.
 *
 * <p>Priority 500 applies this mixin before Better Combat's own {@code Minecraft} mixin (priority
 * 1000), so this HEAD callback runs first and cancels the method before Better Combat can call
 * {@code startUpswing} and play its own slam animation.</p>
 */
@Mixin(value = Minecraft.class, priority = 500)
public abstract class MinecraftContinueAttackMixin {

    @Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
    private void neonarsenal$blockHoldAttack(boolean leftClick, CallbackInfo ci) {
        if (FallSlamState.blockAttacks || ChargeThrustState.blockAttacks) {
            ci.cancel();
        }
    }
}
