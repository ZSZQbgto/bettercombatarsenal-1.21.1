package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: toggles BC's server-side attack disable while the fall windup is active. */
public record FallSlamWindupPayload(boolean windup) implements CustomPacketPayload {

    public static final Type<FallSlamWindupPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "fall_slam_windup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FallSlamWindupPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FallSlamWindupPayload::windup,
                    FallSlamWindupPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
