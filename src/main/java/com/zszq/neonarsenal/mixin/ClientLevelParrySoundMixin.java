package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.fx.BlockShakeEffects;
import com.zszq.neonarsenal.registry.ModSounds;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side hook: the parry sound is broadcast by the server, so every client that receives it
 * locally triggers the hand/camera shake. This avoids a custom network packet entirely.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelParrySoundMixin {

    @Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",
            at = @At("HEAD"))
    private void neonarsenal$onsetParrySound(double x, double y, double z, SoundEvent sound,
                                              SoundSource source, float volume, float pitch,
                                              boolean distanceDelay, long seed, CallbackInfo ci) {
        if (sound == ModSounds.PARRY.get()) {
            BlockShakeEffects.trigger();
            com.zszq.neonarsenal.client.fx.FirstPersonBlockBurst.trySpawn(x, y, z);
        }
    }
}
