package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.fx.TrailFollow;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records Better Combat's slash-trail particles so they can follow the attacking player. */
@Mixin(ParticleEngine.class)
public class ParticleEngineTrailFollowMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void neonarsenal$trackTrailParticle(Particle particle, CallbackInfo ci) {
        if (TrailFollow.isSpawning()) {
            TrailFollow.track(particle);
        }
    }
}
