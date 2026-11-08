package com.zszq.neonarsenal.mixin.bettercombat;

import java.util.List;

import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.api.fx.TrailAppearance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters Better Combat's slash trail (刀光) for the local player while the fall-slam windup is
 * active.
 *
 * <p>{@code SlashParticleUtil.spawnParticles} is the single choke point for both the immediate trail
 * spawned by an attack and the scheduled trail restored from the attack animation packet, so
 * cancelling it here removes Better Combat's trail completely without touching other players.</p>
 */
@Mixin(targets = "net.bettercombat.client.particle.SlashParticleUtil", remap = false)
public class SlashTrailFallSlamMixin {

    @Inject(method = "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void neonarsenal$filterFallSlamTrail(AbstractClientPlayer player, boolean offHand,
                                                        float weaponRange, List<ParticlePlacement> placements,
                                                        TrailAppearance appearance, CallbackInfo ci) {
        if (FallSlamState.waitingForSlam && player == Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
