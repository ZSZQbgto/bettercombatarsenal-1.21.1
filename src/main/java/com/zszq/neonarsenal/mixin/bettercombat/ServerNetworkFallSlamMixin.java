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

    @Inject(method = "handleAttackRequest", at = @At("HEAD"), remap = false, require = 0)
    private static void neonarsenal$markFallSlam(Packets.C2S_AttackRequest packet, MinecraftServer server,
                                                  ServerPlayer player, ServerGamePacketListenerImpl listener,
                                                  CallbackInfo ci) {
        FallSlamHandler.markSlam(player);
    }

    /**
     * The client starts a swing (and its animation) immediately but sends the attack request only at
     * the end of the upswing. Remember the jump-slam animation here so the later request resolves to
     * the same slam attack instead of a normal attack chosen from the server's newer state.
     */
    @Inject(method = "handleAttackAnimation", at = @At("HEAD"), remap = false, require = 0)
    private static void neonarsenal$trackJumpSlamAnimation(Packets.AttackAnimation packet,
                                                           MinecraftServer server,
                                                           ServerPlayer player,
                                                           CallbackInfo ci) {
        com.zszq.neonarsenal.compat.JumpAttackHandler.onClientAttackAnimation(
                player, packet.animationName(), packet.upswingTicks());
    }
}
