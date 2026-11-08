package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: the falling slash resumed from its frozen 1/3 pose four blocks above the ground, so
 * the server plays the claymore's third attack swing sound once at the player's position (matching
 * the actual chopping animation, and nearby players hear it too).
 */
public record FallSlamStrikePayload() implements CustomPacketPayload {

    public static final Type<FallSlamStrikePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "fall_slam_strike"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FallSlamStrikePayload> STREAM_CODEC =
            StreamCodec.unit(new FallSlamStrikePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
