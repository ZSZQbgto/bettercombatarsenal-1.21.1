package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LocalPlayer reads the sneak state from {@code input.shiftKeyDown} in aiStep (see LocalPlayer
 * source), so clearing the input there is what actually suppresses sneaking while blocking; the
 * server-side setShiftKeyDown alone is overwritten by the next input packet.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSneakMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void neonarsenal$suppressSneakWhileBlocking(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.input != null && BlockPose.isBlocking(player)) {
            player.input.shiftKeyDown = false;
        }
    }
}
