package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.network.Packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters Better Combat's own attack animation and swing sound for the local player while the
 * fall-slam windup is active.
 *
 * <p>Better Combat's swing sound packet ({@code Packets.AttackSound}) carries no player id, so an
 * own sound can not be told apart from a nearby player's sound. All Better Combat swing sounds are
 * therefore suppressed for the short windup window; in single-player this is exactly the own sound.
 * The animation packet is filtered by player id only (Better Combat already ignores the local
 * player's own copy, but replay mods do not).</p>
 */
@Mixin(targets = "net.bettercombat.client.ClientNetwork", remap = false)
public class ClientNetworkFallSlamMixin {

    @Inject(method = "handleAttackAnimation", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void neonarsenal$blockOwnAnimation(Packets.AttackAnimation packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (FallSlamState.waitingForSlam && player != null && packet.playerId() == player.getId()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAttackSound", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void neonarsenal$blockOwnSound(Packets.AttackSound packet, CallbackInfo ci) {
        if (FallSlamState.waitingForSlam) {
            ci.cancel();
        }
    }
}
