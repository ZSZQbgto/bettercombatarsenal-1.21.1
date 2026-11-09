package com.zszq.neonarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gives the rendered rider a small vertical bob that follows the mount's gait.
 *
 * <p>The mounted player already follows the mount's actual position (including terrain and jumps),
 * but the mount's walk animation never moves its own body up and down, so the rider looks frozen on
 * a galloping horse. This applies a subtle world-space heave in front of the player renderer, scaled
 * by the mount's walk speed and phase, so the character rises and falls with the horse like a real
 * rider. Boats, minecarts and other non-living vehicles are unaffected.</p>
 */
public final class RiderBob {

    /** Peak vertical offset in blocks at full gait speed (a bit over one model pixel). */
    private static final float AMPLITUDE = 0.08F;
    /** The rider heaves twice per gait cycle, matching the stride rhythm. */
    private static final float FREQUENCY = 0.6662F * 2.0F;

    private RiderBob() {
    }

    public static void apply(AbstractClientPlayer player, float partialTicks, PoseStack poseStack) {
        if (player.isSpectator()) {
            return;
        }
        if (!(player.getVehicle() instanceof LivingEntity mount)) {
            return;
        }
        float speed = Mth.clamp(mount.walkAnimation.speed(partialTicks), 0.0F, 1.0F);
        if (speed <= 0.02F) {
            return;
        }
        float phase = mount.walkAnimation.position(partialTicks);
        float bob = Mth.sin(phase * FREQUENCY) * AMPLITUDE * speed;
        poseStack.translate(0.0D, bob, 0.0D);
    }
}
