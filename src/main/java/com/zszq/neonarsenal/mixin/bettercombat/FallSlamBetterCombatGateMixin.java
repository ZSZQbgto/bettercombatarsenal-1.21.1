package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.api.WeaponAttributes;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Defence in depth: gates Better Combat's client attack execution while the fall-slam holds attacks.
 *
 * <p>Better Combat's {@code startAttack}/{@code continueAttack} HEAD handlers must be cancelled
 * earlier in the same method to stop a swing from ever being created (priority 500 handles that),
 * but this gate makes the guarantee independent of any HEAD-injection ordering: no swing can start
 * and no queued swing can execute (which would deal a local hit, crack a swing sound and spawn a
 * slash trail) while {@link FallSlamState#blockAttacks} is set.</p>
 *
 * <p>{@code require = 0} keeps this tolerant of Better Combat internals being renamed in another
 * version: the hard filters above still apply.</p>
 */
@Mixin(value = Minecraft.class, priority = 2000, remap = false)
public class FallSlamBetterCombatGateMixin {

    /** Blocks Better Combat from creating a new client swing (and therefore its slam animation). */
    @Inject(method = "startUpswing", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neonarsenal$blockUpswing(WeaponAttributes attributes, CallbackInfo ci) {
        if (FallSlamState.blockAttacks) {
            ci.cancel();
        }
    }

    /** Blocks a swing that was already queued before the fall slam from executing its hit. */
    @Inject(method = "performAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neonarsenal$blockPerformAttack(CallbackInfo ci) {
        if (FallSlamState.blockAttacks) {
            ci.cancel();
        }
    }
}
