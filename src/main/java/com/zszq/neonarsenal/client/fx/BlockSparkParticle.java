package com.zszq.neonarsenal.client.fx;

import com.zszq.neonarsenal.compat.BlockSparkOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Block spark with the weapon's particle colour; glows only when a glow ink sac was forged. */
public final class BlockSparkParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final boolean glow;

    private BlockSparkParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet sprites,
                               int color, boolean glow) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.glow = glow;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.hasPhysics = false;
        this.quadSize = 0.075F;
        this.lifetime = 12;
        this.setSpriteFromAge(sprites);
        this.setColor(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
        this.setAlpha(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.setAlpha(Math.max(0.0F, 1.0F - (float) this.age / (float) this.lifetime));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.glow ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<BlockSparkOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(BlockSparkOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new BlockSparkParticle(level, x, y, z, vx, vy, vz, this.sprites,
                    options.color(), options.glow());
        }
    }
}
