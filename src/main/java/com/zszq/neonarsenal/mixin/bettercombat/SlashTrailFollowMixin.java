package com.zszq.neonarsenal.mixin.bettercombat;

import java.util.List;

import com.zszq.neonarsenal.client.fx.TrailFollow;

import net.bettercombat.api.fx.TrailAppearance;

import net.minecraft.client.player.AbstractClientPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks Better Combat's slash-trail spawn so only its own trail particles follow the player. */
@Mixin(targets = "net.bettercombat.client.particle.SlashParticleUtil", remap = false)
public class SlashTrailFollowMixin {

    @Inject(method = "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private static void neonarsenal$beginTrailFollow(AbstractClientPlayer player, boolean offHand,
                                                     float weaponRange, List<?> placements,
                                                     TrailAppearance appearance, CallbackInfo ci) {
        TrailFollow.begin(player);
    }

    @Inject(method = "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private static void neonarsenal$endTrailFollow(AbstractClientPlayer player, boolean offHand,
                                                   float weaponRange, List<?> placements,
                                                   TrailAppearance appearance, CallbackInfo ci) {
        TrailFollow.end();
    }
}
