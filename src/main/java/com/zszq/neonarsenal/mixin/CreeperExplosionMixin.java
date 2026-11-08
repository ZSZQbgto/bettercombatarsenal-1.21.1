package com.zszq.neonarsenal.mixin;

import net.minecraft.world.entity.monster.Creeper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The creeper spawned by the creeper-head forge effect must never use the vanilla explosion:
 * the mod performs its own terrain-free, hostile-only explosion. The vanilla swell is still used
 * for the white flash, so this cancels only the final vanilla explosion for tagged forge creepers.
 */
@Mixin(Creeper.class)
public abstract class CreeperExplosionMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void neonarsenal$cancelForgeCreeperExplosion(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (creeper.getTags().contains("bettercombat_neonarsenal_visual_creeper")) {
            ci.cancel();
        }
    }
}
