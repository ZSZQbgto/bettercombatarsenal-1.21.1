package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: toggles Better Combat's attack channel while a charge thrust windup is active. */
public record ChargeThrustWindupPayload(boolean active) implements CustomPacketPayload {

    public static final Type<ChargeThrustWindupPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "charge_thrust_windup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeThrustWindupPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ChargeThrustWindupPayload::active,
                    ChargeThrustWindupPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
