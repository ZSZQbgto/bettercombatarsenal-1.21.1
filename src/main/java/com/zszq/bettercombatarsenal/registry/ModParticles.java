package com.zszq.bettercombatarsenal.registry;

import com.zszq.bettercombatarsenal.BetterCombatArsenal;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types registered by the mod.
 *
 * <p>{@code glow_spark} is the small full-bright spark emitted along the blade while a Better
 * Combat weapon swing is in progress. Its sprite is the vanilla {@code glow} particle texture and
 * its size/lifetime are read from the client configuration when the particle is spawned.</p>
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BetterCombatArsenal.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_SPARK =
            PARTICLE_TYPES.register("glow_spark", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
