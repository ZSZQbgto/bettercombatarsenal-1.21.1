package com.zszq.neonarsenal.registry;

import com.zszq.neonarsenal.compat.BlockSparkOptions;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.MapCodec;

/** Particle type carrying a per-burst colour and glow flag for block sparks. */
public final class BlockSparkParticleType extends ParticleType<BlockSparkOptions> {

    public BlockSparkParticleType() {
        super(false);
    }

    @Override
    public MapCodec<BlockSparkOptions> codec() {
        return BlockSparkOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, BlockSparkOptions> streamCodec() {
        return BlockSparkOptions.STREAM_CODEC;
    }
}
