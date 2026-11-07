package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.client.compat.ForgeTrailAppearance;
import com.zszq.neonarsenal.item.Forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Better Combat's own slash trail use the forged colour of the attacking weapon instead of
 * the data-driven trail appearance. Only forged arsenal weapons are affected.
 */
@Mixin(targets = "net.bettercombat.client.particle.SlashParticleUtil", remap = false)
public class SlashParticleUtilMixin {

    @Inject(method = "appearanceFor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bettercombat_neonarsenal$forgeAppearance(Player player, ItemStack stack,
                                                            CallbackInfoReturnable<Object> callback) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!Forge.glowEnabled(stack)) {
            // Without the glow-ink-sac forge the BC trail keeps its original data-driven look.
            return;
        }
        Object appearance = ForgeTrailAppearance.of(stack);
        if (appearance != null) {
            callback.setReturnValue(appearance);
        }
    }
}
