package com.zszq.neonarsenal.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Small full-bright spark used for the blade trail particles.
 *
 * <p>Size and lifetime come from {@code weapon_glow.json} ({@code trail.particleScale},
 * {@code trail.particleLifetimeTicks}); the default lifetime is 0.5 s. The particle uses the
 * vanilla {@code glow} sprite, ignores world light and fades out over its lifetime so the sparks
 * appear and disappear naturally along the blade.</p>
 */
public final class GlowSparkParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    /**
     * Spawn parameters for the next particle. {@link net.minecraft.world.level.Level#addParticle}
     * creates the particle synchronously, so the emitter can pick a size/lifetime per spark
     * (attack sparks are short-lived, movement sparks linger for a few seconds).
     */
    public static float nextScale = 0.06F;
    public static int nextLifetime = 10;
    /** Per-spawn RGBA (0..1) used for the next particle. */
    public static float nextRed = 1.0F;
    public static float nextGreen = 1.0F;
    public static float nextBlue = 1.0F;

    private GlowSparkParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites,
                              float scale, int lifetime, float red, float green, float blue) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.hasPhysics = false;
        this.quadSize = scale;
        this.lifetime = Math.max(1, lifetime);
        this.setSpriteFromAge(sprites);
        this.setColor(red * nextRed, green * nextGreen, blue * nextBlue);
        this.setAlpha(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        float remaining = 1.0F - (float) this.age / (float) this.lifetime;
        this.setAlpha(Math.max(0.0F, remaining));
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        return this.quadSize * Math.max(0.25F, 1.0F - 0.5F * progress);
    }

    @Override
    protected int getLightColor(float partialTick) {
        // Full-bright: the sparks are self-lit even in the dark or at night.
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider registered for one {@code bettercombat_neonarsenal:glow_*} spark type. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;
        private final float red;
        private final float green;
        private final float blue;
        private final float scaleMultiplier;

        public Provider(SpriteSet sprites) {
            this(sprites, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        public Provider(SpriteSet sprites, float red, float green, float blue,
                        float scaleMultiplier) {
            this.sprites = sprites;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.scaleMultiplier = scaleMultiplier;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowSparkParticle(level, x, y, z, vx, vy, vz, this.sprites,
                    nextScale * this.scaleMultiplier, nextLifetime, this.red, this.green, this.blue);
        }
    }
}
