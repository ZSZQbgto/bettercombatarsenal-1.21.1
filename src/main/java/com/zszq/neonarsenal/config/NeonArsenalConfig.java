package com.zszq.neonarsenal.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config exposed in the mod-list config screen with two sliders: particle density and blade
 * glow intensity. Both are global multipliers (1.0 = default).
 */
public final class NeonArsenalConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue PARTICLE_DENSITY;
    private static final ModConfigSpec.DoubleValue BLADE_GLOW_INTENSITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        PARTICLE_DENSITY = builder
                .comment("Multiplier for all weapon spark particle counts (slider in the mod list).")
                .translation("bettercombat_neonarsenal.configuration.particle_density")
                .defineInRange("particle_density", 1.0D, 0.0D, 3.0D);
        BLADE_GLOW_INTENSITY = builder
                .comment("Multiplier for the blade glow overlay brightness (slider in the mod list).")
                .translation("bettercombat_neonarsenal.configuration.blade_glow_intensity")
                .defineInRange("blade_glow_intensity", 1.0D, 0.0D, 1.0D);
        SPEC = builder.build();
    }

    private NeonArsenalConfig() {
    }

    public static void setParticleDensity(double value) {
        PARTICLE_DENSITY.set(Math.max(0.0D, Math.min(3.0D, value)));
    }

    public static void setBladeGlowIntensity(double value) {
        BLADE_GLOW_INTENSITY.set(Math.max(0.0D, Math.min(1.0D, value)));
    }

    public static void save() {
        SPEC.save();
    }

    public static double particleDensity() {
        return PARTICLE_DENSITY == null ? 1.0D : PARTICLE_DENSITY.get();
    }

    public static double bladeGlowIntensity() {
        return BLADE_GLOW_INTENSITY == null ? 1.0D : BLADE_GLOW_INTENSITY.get();
    }
}
