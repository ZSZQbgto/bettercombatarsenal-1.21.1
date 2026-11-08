package com.zszq.neonarsenal.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Looping elytra gliding wind played during the falling-slash fall. Volume and pitch follow the
 * player's actual downward speed, so the wind rises as the player falls faster and fades out when
 * the fall ends.
 *
 * <p>{@link #canStartSilent()} must be overridden: the sound starts at zero volume (so there is no
 * pop when falling slowly) and the sound engine otherwise skips zero-volume sounds entirely, which
 * would leave it silent forever because a skipped instance is never ticked.</p>
 */
final class FallSlamWindSound extends AbstractTickableSoundInstance {

    /** Approximate vanilla terminal falling speed in blocks per tick. */
    private static final double TERMINAL_FALL_SPEED = 3.92D;

    private final LocalPlayer player;
    private int age;

    FallSlamWindSound(LocalPlayer player) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved()) {
            stopWind();
            return;
        }
        this.age++;
        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();
        double fallSpeed = Math.max(0.0D, -this.player.getDeltaMovement().y);
        float intensity = Mth.clamp((float) (fallSpeed / TERMINAL_FALL_SPEED), 0.0F, 1.0F);
        // Quick fade in, then volume follows the fall speed (silent when barely moving).
        float fadeIn = Math.min(1.0F, this.age / 4.0F);
        this.volume = Mth.clamp(0.55F * intensity, 0.0F, 0.6F) * fadeIn;
        this.pitch = 0.85F + 0.3F * intensity;
    }

    /** Asks the sound manager to remove this instance. */
    void stopWind() {
        this.volume = 0.0F;
        this.stop();
    }
}
