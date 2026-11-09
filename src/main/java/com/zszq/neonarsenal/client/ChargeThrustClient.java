package com.zszq.neonarsenal.client;

import com.zszq.neonarsenal.compat.ChargeThrustStrikePayload;
import com.zszq.neonarsenal.compat.ChargeThrustWindupPayload;
import com.zszq.neonarsenal.compat.ChargeThrustState;
import com.zszq.neonarsenal.compat.JumpAttackHandler;
import com.zszq.neonarsenal.mixin.MinecraftAttackAccessor;
import com.zszq.neonarsenal.weapon.WeaponDefinition.Ability;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Mounted charge thrust (冲锋前刺) for the spear.
 *
 * <p><b>Eligibility.</b> The main hand holds a weapon with the {@link Ability#CHARGE_THRUST} skill
 * (the spear) while the player rides a living mount. Elytra gliding does not trigger it.</p>
 *
 * <p><b>Short tap.</b> One normal spear thrust attack, exactly like a regular attack (Better Combat's
 * own attack is let through for the release).</p>
 *
 * <p><b>Long press.</b> After 0.15 s the spear thrust animation is played manually on Better Combat's
 * attack animation layer and frozen at one third of its length; the server is told to disable Better
 * Combat's attack channel, and while the player keeps charging the server damages every creature the
 * player runs into along the attack direction. Releasing resumes the animation to completion and
 * tells the server to apply the released strike; the pose then fades to idle over 0.3 s.</p>
 */
public final class ChargeThrustClient {

    private static final int LONG_PRESS_TICKS = 3;   // 0.15 s hold before committing
    private static final int FADE_TICKS = 6;         // 0.3 s fade to idle
    private static final int LOCK_TICKS = 6;         // 0.3 s attack lock during the fade
    private static final int ABORT_FADE_TICKS = 4;   // 0.2 s fade back when aborted
    private static final int MAX_RELEASE_TICKS = 60; // safety net if Better Combat internals are lost

    private enum Phase {
        IDLE,
        /** Attack key pressed, waiting for the 0.15 s long-press decision. */
        PENDING,
        /** Thrust animation frozen at 1/3 while the key is held. */
        CHARGING,
        /** Released: the animation completes before the fade. */
        RELEASE,
        /** Final frame reached: fade to idle and matching attack lock. */
        LOCK
    }

    private static Phase phase = Phase.IDLE;
    private static LocalPlayer trackedPlayer;
    private static boolean wasAttackDown;
    private static int keyDownTicks;
    private static int releaseTicks;
    private static int lockTicks;
    private static boolean serverWindup;

    private ChargeThrustClient() {
    }

    /** {@return true while the charge owns Better Combat's attack animation layer}. */
    public static boolean isCharging() {
        return phase == Phase.CHARGING || phase == Phase.RELEASE || phase == Phase.LOCK;
    }

    /** Runs before vanilla processes the attack key, so the filters are in place first. */
    public static void preTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            trackedPlayer = null;
            resetLocal();
            return;
        }
        if (player != trackedPlayer) {
            trackedPlayer = player;
            resetLocal();
        }
        if (player.isDeadOrDying()) {
            abort(player);
            return;
        }
        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (phase == Phase.RELEASE || phase == Phase.LOCK) {
            holdFilters();
            wasAttackDown = attackDown;
            return;
        }
        if (!eligible(player)) {
            if (phase != Phase.IDLE) {
                abort(player);
            }
            wasAttackDown = attackDown;
            return;
        }
        boolean pressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        switch (phase) {
            case IDLE -> {
                if (pressed) {
                    phase = Phase.PENDING;
                    keyDownTicks = 1;
                    holdFilters();
                    // Drop a Better Combat swing started before the press so its delayed hit and
                    // animation do not leak into the charge.
                    FallSlamAnimation.clearBetterCombatSwing();
                }
            }
            case PENDING -> {
                if (!attackDown) {
                    shortTapAttack(minecraft);
                    return;
                }
                keyDownTicks++;
                holdFilters();
                if (keyDownTicks >= LONG_PRESS_TICKS) {
                    startCharge(player);
                }
            }
            case CHARGING -> {
                holdFilters();
                if (!attackDown) {
                    release(player);
                }
            }
            default -> {
            }
        }
    }

    /** Runs after the player ticked, so animation progress is up to date. */
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            trackedPlayer = null;
            resetLocal();
            return;
        }
        if (player != trackedPlayer) {
            trackedPlayer = player;
            resetLocal();
        }
        if (player.isDeadOrDying()) {
            abort(player);
            return;
        }
        switch (phase) {
            case CHARGING -> {
                holdFilters();
                if (!eligible(player)) {
                    abort(player);
                    return;
                }
                ChargeThrustAnimation.tickPause(player);
            }
            case RELEASE -> {
                holdFilters();
                releaseTicks++;
                if (releaseTicks > MAX_RELEASE_TICKS || ChargeThrustAnimation.isFinished(player)) {
                    beginFade(player);
                }
            }
            case LOCK -> {
                holdFilters();
                lockTicks--;
                if (lockTicks <= 0) {
                    finish();
                }
            }
            case IDLE, PENDING -> {
                if (phase == Phase.IDLE) {
                    clearFilters();
                }
            }
            default -> {
            }
        }
    }

    /** {@return whether the local player can currently start a charge thrust}. */
    private static boolean eligible(LocalPlayer player) {
        if (!JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.CHARGE_THRUST)) {
            return false;
        }
        if (!(player.getVehicle() instanceof LivingEntity)) {
            return false;
        }
        return !player.isUsingItem() && !player.isSpectator()
                && !player.getAbilities().flying && !player.isDeadOrDying();
    }

    private static void startCharge(LocalPlayer player) {
        phase = Phase.CHARGING;
        holdFilters();
        FallSlamAnimation.clearBetterCombatSwing();
        ChargeThrustAnimation.onExternalStart(player);
        setServerWindup(true);
    }

    private static void release(LocalPlayer player) {
        phase = Phase.RELEASE;
        releaseTicks = 0;
        holdFilters();
        ChargeThrustAnimation.resumeOriginal(player);
        send(new ChargeThrustStrikePayload(true));
    }

    private static void beginFade(LocalPlayer player) {
        phase = Phase.LOCK;
        lockTicks = LOCK_TICKS;
        holdFilters();
        ChargeThrustAnimation.finishAndFade(player, FADE_TICKS);
    }

    private static void finish() {
        clearFilters();
        ChargeThrustAnimation.reset();
        setServerWindup(false);
        phase = Phase.IDLE;
        wasAttackDown = attackKeyDown();
        keyDownTicks = 0;
        releaseTicks = 0;
    }

    private static void abort(LocalPlayer player) {
        if (phase == Phase.CHARGING || phase == Phase.RELEASE) {
            ChargeThrustAnimation.stop(player, ABORT_FADE_TICKS);
        }
        clearFilters();
        ChargeThrustAnimation.reset();
        setServerWindup(false);
        phase = Phase.IDLE;
        wasAttackDown = attackKeyDown();
        keyDownTicks = 0;
        releaseTicks = 0;
        lockTicks = 0;
    }

    /** A short tap performs one normal attack through the filters. */
    private static void shortTapAttack(Minecraft minecraft) {
        phase = Phase.IDLE;
        clearFilters();
        ChargeThrustState.allowNormalAttack = true;
        try {
            ((MinecraftAttackAccessor) minecraft).neonarsenal$startAttack();
        } catch (Throwable ignored) {
            // Optional normal attack fallback.
        } finally {
            ChargeThrustState.allowNormalAttack = false;
        }
        wasAttackDown = attackKeyDown();
        keyDownTicks = 0;
    }

    private static void resetLocal() {
        phase = Phase.IDLE;
        clearFilters();
        ChargeThrustAnimation.reset();
        serverWindup = false;
        wasAttackDown = false;
        keyDownTicks = 0;
        releaseTicks = 0;
        lockTicks = 0;
    }

    private static boolean attackKeyDown() {
        return Minecraft.getInstance().options.keyAttack.isDown();
    }

    private static void holdFilters() {
        ChargeThrustState.blockAttacks = true;
    }

    private static void clearFilters() {
        ChargeThrustState.blockAttacks = false;
        ChargeThrustState.allowAnimation = false;
        ChargeThrustState.allowNormalAttack = false;
    }

    private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        try {
            PacketDistributor.sendToServer(payload);
        } catch (Throwable ignored) {
            // Not connected; nothing to restore remotely.
        }
    }

    private static void setServerWindup(boolean windup) {
        if (serverWindup == windup) {
            return;
        }
        serverWindup = windup;
        send(new ChargeThrustWindupPayload(windup));
    }
}
