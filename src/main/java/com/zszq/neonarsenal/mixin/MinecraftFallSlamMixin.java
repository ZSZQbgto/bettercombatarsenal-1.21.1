package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.FallSlamClient;
import com.zszq.neonarsenal.compat.FallSlamState;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks the first attack input while the fall-slam windup holds attacks.
 *
 * <p>Priority 500 applies this mixin <b>before</b> Better Combat's own {@code Minecraft} mixin
 * (priority 1000). Mixin applies lower priorities first and inserts same-point HEAD callbacks in
 * application order, so this callback cancels the method before Better Combat's
 * {@code pre_doAttack} can start its upswing (which would play Better Combat's own slam).</p>
 */
@Mixin(value = Minecraft.class, priority = 500)
public abstract class MinecraftFallSlamMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void neonarsenal$suppressFallAttack(CallbackInfoReturnable<Boolean> ci) {
        if (FallSlamClient.isTriggeringAttack()) {
            return;
        }
        if (FallSlamState.blockAttacks) {
            ci.setReturnValue(false);
        }
    }
}
