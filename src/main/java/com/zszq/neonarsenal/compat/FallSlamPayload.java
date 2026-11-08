package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: a falling slash committed with this fall distance, so the server can settle it. */
public record FallSlamPayload(float fallBlocks) implements CustomPacketPayload {

    public static final Type<FallSlamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "fall_slam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FallSlamPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, FallSlamPayload::fallBlocks, FallSlamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
