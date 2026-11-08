package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.FallSlamHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import net.bettercombat.network.Packets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks a fall-slam on the server when Better Combat receives its attack request. */
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public class ServerNetworkFallSlamMixin {

    @Inject(method = "handleAttackRequest", at = @At("HEAD"), remap = false)
    private static void neonarsenal$markFallSlam(Packets.C2S_AttackRequest packet, MinecraftServer server,
                                                  ServerPlayer player, ServerGamePacketListenerImpl listener,
                                                  CallbackInfo ci) {
        FallSlamHandler.markSlam(player);
    }
}
