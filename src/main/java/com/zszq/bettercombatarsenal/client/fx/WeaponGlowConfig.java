package com.zszq.bettercombatarsenal.client.fx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszq.bettercombatarsenal.BetterCombatArsenal;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Client-side, user-editable blade-glow configuration.
 *
 * <p>File: {@code config/bettercombatarsenal/weapon_glow.json}. It is created with defaults on the
 * first client launch and re-applied within about a second after it is edited. The file only
 * controls the client-side visual effect; it is never read by or synced to a dedicated server.</p>
 */
public final class WeaponGlowConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("bettercombatarsenal")
            .resolve("weapon_glow.json");

    private static WeaponGlowConfig instance = defaults();
    private static long lastModified = Long.MIN_VALUE;

    /** Master switch for all blade effects. */
    public boolean enabled = true;
    public Glow glow = new Glow();
    public Trail trail = new Trail();
    public Motion motion = new Motion();
    public Idle idle = new Idle();

    private transient float[] tint = new float[]{1.0F, 1.0F, 1.0F};

    public static WeaponGlowConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                writeDefault();
            }
            lastModified = Files.getLastModifiedTime(PATH).toMillis();
            WeaponGlowConfig loaded = GSON.fromJson(Files.readString(PATH), WeaponGlowConfig.class);
            if (loaded == null) {
                loaded = defaults();
            }
            loaded.normalize();
            instance = loaded;
        } catch (Exception e) {
            BetterCombatArsenal.LOGGER.error("Failed to load blade glow config from {}; using defaults", PATH, e);
            instance = defaults();
        }
    }

    /** Called periodically from the client tick loop; cheap when the file did not change. */
    public static void reloadIfChanged() {
        try {
            if (!Files.exists(PATH)) {
                writeDefault();
                load();
                return;
            }
            long modified = Files.getLastModifiedTime(PATH).toMillis();
            if (modified != lastModified) {
                load();
            }
        } catch (Exception e) {
            // Keep the last good configuration and avoid spamming the log every second.
            lastModified = Long.MIN_VALUE;
        }
    }

    private static WeaponGlowConfig defaults() {
        WeaponGlowConfig config = new WeaponGlowConfig();
        config.normalize();
        return config;
    }

    private static void writeDefault() throws Exception {
        Files.createDirectories(PATH.getParent());
        WeaponGlowConfig config = defaults();
        Files.writeString(PATH, GSON.toJson(config));
    }

    private void normalize() {
        if (glow == null) {
            glow = new Glow();
        }
        if (trail == null) {
            trail = new Trail();
        }
        if (motion == null) {
            motion = new Motion();
        }
        if (idle == null) {
            idle = new Idle();
        }
        glow.alpha = Math.max(0.0F, Math.min(1.0F, glow.alpha));
        if (glow.mode == null || glow.mode.isBlank()) {
            glow.mode = "auto";
        }
        glow.mode = glow.mode.trim().toLowerCase(java.util.Locale.ROOT);
        if (trail.particle == null || trail.particle.isBlank()) {
            trail.particle = "bettercombatarsenal:glow_spark";
        }
        trail.particlesPerTick = Math.max(0, Math.min(16, trail.particlesPerTick));
        trail.particleLifetimeTicks = Math.max(1, Math.min(40, trail.particleLifetimeTicks));
        trail.particleScale = Math.max(0.01D, Math.min(0.5D, trail.particleScale));
        trail.spread = Math.max(0.0D, Math.min(0.5D, trail.spread));
        trail.speed = Math.max(0.0D, Math.min(0.2D, trail.speed));
        motion.particlesPerTick = Math.max(0, Math.min(8, motion.particlesPerTick));
        motion.intervalTicks = Math.max(1, Math.min(20, motion.intervalTicks));
        motion.particleLifetimeTicks = Math.max(10, Math.min(120, motion.particleLifetimeTicks));
        motion.particleScale = Math.max(0.01D, Math.min(0.5D, motion.particleScale));
        motion.minSpeed = Math.max(0.0D, Math.min(1.0D, motion.minSpeed));
        motion.spread = Math.max(0.0D, Math.min(0.5D, motion.spread));
        motion.speed = Math.max(0.0D, Math.min(0.2D, motion.speed));
        idle.intervalTicks = Math.max(1, Math.min(60, idle.intervalTicks));
        idle.particlesPerEmit = Math.max(0, Math.min(4, idle.particlesPerEmit));
        idle.particleLifetimeTicks = Math.max(10, Math.min(120, idle.particleLifetimeTicks));
        idle.particleScale = Math.max(0.01D, Math.min(0.5D, idle.particleScale));
        idle.spread = Math.max(0.0D, Math.min(0.5D, idle.spread));
        idle.speed = Math.max(0.0D, Math.min(0.2D, idle.speed));
        trail.variants = cleanVariants(trail.variants);
        motion.variants = cleanVariants(motion.variants);
        idle.variants = cleanVariants(idle.variants);
        tint = parseColor(glow.tint);
    }

    private static List<String> cleanVariants(List<String> variants) {
        if (variants == null) {
            return new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (String variant : variants) {
            if (variant != null && !variant.isBlank()) {
                cleaned.add(variant.trim());
            }
        }
        return cleaned;
    }

    /** Tint multiplied with the supplied blade mask texture, in linear-ish 0..1 RGB. */
    public float[] tintColor() {
        return tint;
    }

    private static float[] parseColor(String hex) {
        if (hex != null) {
            String value = hex.trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.length() == 6) {
                try {
                    return new float[]{
                            Integer.parseInt(value.substring(0, 2), 16) / 255.0F,
                            Integer.parseInt(value.substring(2, 4), 16) / 255.0F,
                            Integer.parseInt(value.substring(4, 6), 16) / 255.0F};
                } catch (NumberFormatException ignored) {
                    // fall through to white
                }
            }
        }
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    public static final class Glow {
        public boolean enabled = true;
        public float alpha = 0.85F;
        /** Additive blending makes the blade read as self-lit; false uses normal translucency. */
        public boolean additive = true;
        /**
         * Render mode: {@code auto} (additive normally, emissive layer while an Iris shader pack is
         * active), or a fixed {@code additive} / {@code translucent} / {@code emissive} override.
         */
        public String mode = "auto";
        /** Tint multiplied with the blade mask, "#RRGGBB". */
        public String tint = "#FFFFFF";
    }

    /**
     * Extra glowing sparks emitted along the blade while a Better Combat swing is active. The blade
     * glow on Better Combat's own slash trail comes from {@code trail_appearance} in the
     * {@code weapon_attributes} data files, not from this section.
     */
    public static final class Trail {
        public boolean particles = true;
        /**
         * Particle id. The default is the mod's own small full-bright spark; any other registered
         * particle id can be used instead (scale/lifetime then only apply to glow_* sparks).
         */
        public String particle = "bettercombatarsenal:glow_spark";
        /**
         * Optional list of glow_* particle ids; if not empty, one is picked at random for every
         * spark, which mixes the different spark styles. Falls back to {@link #particle}.
         */
        public List<String> variants = new ArrayList<>(List.of(
                "bettercombatarsenal:glow_sparkle",
                "bettercombatarsenal:glow_crit",
                "bettercombatarsenal:glow_spark"));
        /** Sparks emitted per client tick while a swing is active (higher = denser). */
        public int particlesPerTick = 5;
        /** Spark lifetime in client ticks; 10 ticks = 0.5 s. */
        public int particleLifetimeTicks = 10;
        /** Spark size (vanilla glow sprite quad size); smaller = finer particles. */
        public double particleScale = 0.05D;
        /** Random position jitter in blocks. */
        public double spread = 0.08D;
        /** Random velocity in blocks per tick. */
        public double speed = 0.015D;
    }

    /**
     * Small number of long-lived sparks left behind where the blade sweeps while the player is
     * moving. They drift almost nowhere and fade out over a few seconds.
     */
    public static final class Motion {
        public boolean particles = true;
        /** Sparks per emission, emitted every {@link #intervalTicks} ticks. */
        public int particlesPerTick = 1;
        /** Ticks between emissions (2 = every 0.1 s). */
        public int intervalTicks = 2;
        /** Spark lifetime in client ticks; 50 ticks = 2.5 s. */
        public int particleLifetimeTicks = 50;
        /** Optional random mix of glow_* particle ids (see {@link Trail#variants}). */
        public List<String> variants = new ArrayList<>(List.of(
                "bettercombatarsenal:glow_glitter",
                "bettercombatarsenal:glow_spark"));
        /** Spark size (vanilla glow sprite quad size). */
        public double particleScale = 0.04D;
        /** Minimum horizontal player speed (blocks/tick) needed to emit. */
        public double minSpeed = 0.05D;
        /** Random position jitter in blocks. */
        public double spread = 0.04D;
        /** Random velocity in blocks per tick (near zero = stays where it was created). */
        public double speed = 0.0D;
    }

    /**
     * A very light ambient trickle of sparks at the blade that also runs while the weapon is idle:
     * in hand, on the ground, in an item frame, or on any shelf/modded display context. This keeps
     * the blade faintly "alive" even when nothing is moving.
     */
    public static final class Idle {
        public boolean particles = true;
        /** One emission every this many client ticks (8 = about 2.5x per second). */
        public int intervalTicks = 8;
        /** Sparks per emission. */
        public int particlesPerEmit = 1;
        /** Spark lifetime in client ticks (40 = 2 s). */
        public int particleLifetimeTicks = 40;
        /** Optional random mix of glow_* particle ids (see {@link Trail#variants}). */
        public List<String> variants = new ArrayList<>(List.of(
                "bettercombatarsenal:glow_spark",
                "bettercombatarsenal:glow_glitter",
                "bettercombatarsenal:glow_magic"));
        /** Spark size; keep this small so the idle effect stays subtle. */
        public double particleScale = 0.04D;
        /** Random position jitter in blocks. */
        public double spread = 0.03D;
        /** Random velocity in blocks per tick. */
        public double speed = 0.0D;
    }
}
