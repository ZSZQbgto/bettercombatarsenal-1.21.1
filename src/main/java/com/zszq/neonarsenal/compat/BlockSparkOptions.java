package com.zszq.neonarsenal.compat;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zszq.neonarsenal.registry.ModParticles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Colored block-spark particle data. The colour follows the current weapon's particle colour and
 * {@code glow} is true when the weapon has a forged glow ink sac.
 */
public record BlockSparkOptions(int color, boolean glow) implements ParticleOptions {

    public static final MapCodec<BlockSparkOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("color").forGetter(BlockSparkOptions::color),
            com.mojang.serialization.Codec.BOOL.fieldOf("glow").forGetter(BlockSparkOptions::glow)
    ).apply(instance, BlockSparkOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockSparkOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, BlockSparkOptions::color,
                    ByteBufCodecs.BOOL, BlockSparkOptions::glow,
                    BlockSparkOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BLOCK_SPARK.get();
    }
}
