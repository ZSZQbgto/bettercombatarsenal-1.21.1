package com.zszq.neonarsenal.mixin.pal;

import com.zszq.neonarsenal.client.fx.CapeRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports the Player Animator Library animation as inactive during the cape render, so its
 * {@code CapeLayerMixin} skips the "attach the cape to the animated torso" transform and the cape
 * renders exactly like vanilla.
 *
 * <p>{@code isActive()} is declared on {@code AnimationProcessor} and called virtually on the
 * {@code AnimationApplier} subclass, so the mixin has to target the declaring class.</p>
 *
 * <p>The flag is only set inside {@code CapeLayer.render} (see {@link CapeRenderState}), so actual
 * animation playback is unaffected.</p>
 */
@Mixin(targets = "dev.kosmx.playerAnim.core.impl.AnimationProcessor", remap = false)
public class AnimationApplierCapeMixin {

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neonarsenal$hideCapeAnimation(CallbackInfoReturnable<Boolean> callback) {
        if (CapeRenderState.suppressPalCape) {
            callback.setReturnValue(false);
        }
    }
}
