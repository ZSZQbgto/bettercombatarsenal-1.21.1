package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: the charge thrust was released. The server completes the thrust, applying the
 * relative-speed charge damage to every creature hit by the released strike.
 */
public record ChargeThrustStrikePayload(boolean charged) implements CustomPacketPayload {

    public static final Type<ChargeThrustStrikePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "charge_thrust_strike"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeThrustStrikePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ChargeThrustStrikePayload::charged,
                    ChargeThrustStrikePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
