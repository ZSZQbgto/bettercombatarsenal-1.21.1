package com.zszq.neonarsenal.registry;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types registered by the mod: five different blade spark styles that share the same
 * full-bright, fading behaviour but use different vanilla sprites and tints.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, NeonArsenal.MODID);

    /** Plain star-shaped glow sprite (vanilla {@code glow} texture). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_SPARK =
            PARTICLE_TYPES.register("glow_spark", () -> new SimpleParticleType(true));

    /** Animated end-rod twinkle, warm white. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_GLITTER =
            PARTICLE_TYPES.register("glow_glitter", () -> new SimpleParticleType(true));

    /** Animated firework spark, bright white-blue. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_SPARKLE =
            PARTICLE_TYPES.register("glow_sparkle", () -> new SimpleParticleType(true));

    /** Critical-hit star burst, pale gold, crisp flash. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_CRIT =
            PARTICLE_TYPES.register("glow_crit", () -> new SimpleParticleType(true));

    /** Enchanted-hit sparkle, cool violet-white glint. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_MAGIC =
            PARTICLE_TYPES.register("glow_magic", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
