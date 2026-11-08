package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent to the blocking player each time the parry sound/particle burst actually triggers. */
public record BlockShakePayload() implements CustomPacketPayload {

    public static final BlockShakePayload INSTANCE = new BlockShakePayload();

    public static final Type<BlockShakePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "block_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockShakePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
