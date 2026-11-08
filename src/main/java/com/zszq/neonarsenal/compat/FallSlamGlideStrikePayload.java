package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: the gliding falling slash hit a creature. Carries the struck entity id and the
 * player's impact speed (blocks per tick); the server converts it to the equivalent free-fall height
 * so the glide slash stays balanced with the normal falling slash damage.
 */
public record FallSlamGlideStrikePayload(int targetId, float speed) implements CustomPacketPayload {

    public static final Type<FallSlamGlideStrikePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "fall_slam_glide_strike"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FallSlamGlideStrikePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FallSlamGlideStrikePayload::targetId,
                    ByteBufCodecs.FLOAT, FallSlamGlideStrikePayload::speed,
                    FallSlamGlideStrikePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
