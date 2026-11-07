package com.zszq.bettercombatarsenal.mixin;

import com.zszq.bettercombatarsenal.compat.JumpAttackHandler;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks the player's jump while the jump-attack cooldown is running, so a jump attack cannot be
 * chained by immediately jumping again after landing.
 *
 * <p>Only loaded on the client (listed in the {@code client} section of the mixin config) and only
 * acts for the local player holding an arsenal weapon.</p>
 */
@Mixin(targets = "net.minecraft.world.entity.LivingEntity", remap = false)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true, remap = false)
    private void bettercombatarsenal$blockJump(CallbackInfo callback) {
        if ((Object) this instanceof LocalPlayer player
                && JumpAttackHandler.isJumpBlocked(player)
                && JumpAttackHandler.isHoldingArsenalWeapon(player)) {
            com.zszq.bettercombatarsenal.BetterCombatArsenal.LOGGER.debug(
                    "Jump-attack cooldown: jump blocked");
            callback.cancel();
        }
    }
}
