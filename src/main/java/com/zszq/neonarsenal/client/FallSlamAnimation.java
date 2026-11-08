package com.zszq.neonarsenal.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.zszq.neonarsenal.compat.FallSlamState;

import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-side control of the Better Combat slam animation used as the paused falling-slash windup.
 *
 * <p>Units matter. Better Combat's
 * {@code playAttackAnimation(name, hand, length, upswing)} takes the animation length in <b>ticks</b>
 * (it derives the playback speed as {@code endTick / length}), and
 * {@code stopAttackAnimation(length)} also takes <b>ticks</b> (it does {@code Math.round(length)} for
 * the fade length). Passing {@code 1} as the length therefore made the slam play out in a single
 * tick, and passing {@code 0.3} as the stop length rounded the fade down to zero ticks instead of
 * 0.3 s (six ticks).</p>
 *
 * <p>The animation is played on Better Combat's attack animation layer (priority 2000), so it
 * replaces whatever Better Combat animation was running. While {@link #isControlling()} the
 * animation is protected from Better Combat's own {@code stopAttackAnimation} calls (for example the
 * client swing cancel that runs when the server disables attacks); only this class may stop it.</p>
 */
public final class FallSlamAnimation {

    /** The exact Better Combat animation used for the windup. */
    public static final String SLAM_ANIMATION = "bettercombat:two_handed_slam";

    /** Better Combat derives its own ramp from this upswing fraction. */
    private static final float UPSWING = 0.5F;

    private static boolean controlling;
    private static boolean paused;
    private static boolean frozen;
    private static boolean allowStop;

    private static Method cooldownMethod;
    private static boolean cooldownResolved;

    private FallSlamAnimation() {
    }

    /**
     * Manually starts the slam animation with the player's attack cooldown as its length and freezes
     * it once it reaches one third. This is the only animation allowed through the client filter.
     */
    public static void onExternalStart(LocalPlayer player) {
        if (controlling) {
            return;
        }
        controlling = true;
        paused = false;
        frozen = false;
        // Remove the animation that is currently playing (Better Combat's own airborne jump attack
        // started by the initial press) instantly, with a zero-length fade. Otherwise Better Combat's
        // playAttackAnimation keeps that animation as the fade source: the old animation keeps
        // ticking for the whole 7-tick fade-in, so its remaining downswing/follow-through plays while
        // the windup fades in and looks like a complete Better Combat jump-slam animation right
        // before the 1/3 freeze. With it gone, the new animation is installed without a fade and
        // rises smoothly from the current pose into the 1/3 windup pose.
        allowStop = true;
        try {
            ((PlayerAttackAnimatable) player).stopAttackAnimation(0.0F);
        } catch (Throwable ignored) {
            // Better Combat optional.
        } finally {
            allowStop = false;
        }
        FallSlamState.allowAnimation = true;
        try {
            float length = cooldownTicks(player);
            ((PlayerAttackAnimatable) player).playAttackAnimation(
                    SLAM_ANIMATION, AnimatedHand.TWO_HANDED, length, UPSWING);
            paused = true;
            setSpeed(player, 1.0F);
        } catch (Throwable ignored) {
            // Better Combat missing or its internals changed: degrade to a normal fall.
            controlling = false;
            paused = false;
            frozen = false;
        } finally {
            FallSlamState.allowAnimation = false;
        }
    }

    /** Called every client tick; freezes the animation once it reaches one third of its length. */
    public static void tickPause(LocalPlayer player) {
        if (!controlling || !paused || frozen) {
            return;
        }
        int tick = currentTick(player);
        int end = endTick(player);
        if (tick < 0 || end <= 0) {
            return;
        }
        int freezeAt = Math.max(1, (end + 2) / 3);
        if (tick >= freezeAt) {
            setSpeed(player, 0.0F);
            frozen = true;
        }
    }

    /** Resumes the paused animation at the original speed (1.0), not accelerated. */
    public static void resumeOriginal(LocalPlayer player) {
        if (!controlling) {
            return;
        }
        paused = false;
        frozen = false;
        setSpeed(player, 1.0F);
    }

    /**
     * {@return true once the manually played animation reached its final keyframe (or no longer
     * exists), i.e. the point where it must fade to idle}.
     */
    public static boolean isFinished(LocalPlayer player) {
        Object animation = currentAnimation(player);
        if (animation == null) {
            return true;
        }
        try {
            Object active = animation.getClass().getMethod("isActive").invoke(animation);
            if (!Boolean.TRUE.equals(active)) {
                return true;
            }
            int tick = currentTick(player);
            int end = endTick(player);
            // Start the fade one animation tick before the last keyframe. The slam pose is static
            // from its last movement keyframe to endTick, so this is visually the final frame, and
            // it keeps the fading source animation active for all 6 fade ticks
            // (two_handed_slam: endTick 20 -> stopTick 25, only 5 ticks of animation activity).
            return tick >= 0 && end > 0 && tick >= Math.max(1, end - 1);
        } catch (Throwable ignored) {
            // Do not fade prematurely if Better Combat internals cannot be inspected.
            return false;
        }
    }

    /**
     * Fades the finished pose to idle over the given number of ticks (Better Combat expects ticks,
     * 0.3 s = 6 ticks).
     */
    public static void finishAndFade(LocalPlayer player, float fadeTicks) {
        paused = false;
        frozen = false;
        allowStop = true;
        try {
            // The fade is driven by the same modifier chain as the animation; make sure it is not
            // stuck at the frozen speed while it fades out.
            setSpeed(player, 1.0F);
            ((PlayerAttackAnimatable) player).stopAttackAnimation(fadeTicks);
        } catch (Throwable ignored) {
            // Optional compatibility only.
        } finally {
            allowStop = false;
            controlling = false;
        }
    }

    /** Aborts the windup (attack key released before the slam committed) with a 0.2 s fade. */
    public static void stop(LocalPlayer player, float fadeTicks) {
        if (!controlling) {
            return;
        }
        paused = false;
        frozen = false;
        allowStop = true;
        try {
            setSpeed(player, 1.0F);
            ((PlayerAttackAnimatable) player).stopAttackAnimation(fadeTicks);
        } catch (Throwable ignored) {
            // Optional compatibility only.
        } finally {
            allowStop = false;
            controlling = false;
        }
    }

    /**
     * Forgets the ownership state without touching any animation. Used when the local player entity
     * is replaced (respawn, dimension change, disconnect) and the old animation no longer exists.
     */
    public static void reset() {
        controlling = false;
        paused = false;
        frozen = false;
        allowStop = false;
    }

    /**
     * {@return true while this class owns the attack animation layer}. Used to keep Better Combat
     * from cancelling the manual windup pose.
     */
    public static boolean isControlling() {
        return controlling;
    }

    /** {@return true only for this class' own {@code stopAttackAnimation} call}. */
    public static boolean isAllowingStop() {
        return allowStop;
    }

    /**
     * Drops Better Combat's in-flight client swing. Otherwise its delayed hit could execute
     * (damage, swing sound, slash trail) during the windup and its cancel path would try to stop the
     * manual animation.
     */
    public static void clearBetterCombatSwing() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            clearField(minecraft, "ongoingSwing");
            clearField(minecraft, "upswingStack");
        } catch (Throwable ignored) {
            // Better Combat optional.
        }
    }

    private static void clearField(Object owner, String name) {
        try {
            Field field = findField(owner.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                field.set(owner, null);
            }
        } catch (Throwable ignored) {
            // Field may not exist in this Better Combat version.
        }
    }

    private static float cooldownTicks(LocalPlayer player) {
        if (!cooldownResolved) {
            cooldownResolved = true;
            try {
                Class<?> helper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
                cooldownMethod = helper.getMethod("getAttackCooldownTicksCapped",
                        net.minecraft.world.entity.player.Player.class);
            } catch (Throwable ignored) {
                cooldownMethod = null;
            }
        }
        if (cooldownMethod != null) {
            try {
                float ticks = ((Number) cooldownMethod.invoke(null, player)).floatValue();
                return Math.max(1.0F, ticks);
            } catch (Throwable ignored) {
                // Fall through to the default cooldown.
            }
        }
        return 20.0F;
    }

    private static Object currentAnimation(LocalPlayer player) {
        try {
            Field field = findField(player.getClass(), "attackAnimation");
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object substack = field.get(player);
            if (!(substack instanceof AttackAnimationSubStack)) {
                return null;
            }
            return ((AttackAnimationSubStack) substack).base.getAnimation();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int currentTick(LocalPlayer player) {
        Object animation = currentAnimation(player);
        if (animation == null) {
            return -1;
        }
        try {
            Method method = animation.getClass().getMethod("getCurrentTick");
            return ((Number) method.invoke(animation)).intValue();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int endTick(LocalPlayer player) {
        Object animation = currentAnimation(player);
        if (animation == null) {
            return -1;
        }
        try {
            Object data = animation.getClass().getMethod("getData").invoke(animation);
            return data.getClass().getField("endTick").getInt(data);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void setSpeed(LocalPlayer player, float speed) {
        try {
            Field field = findField(player.getClass(), "attackAnimation");
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            Object substack = field.get(player);
            if (!(substack instanceof AttackAnimationSubStack animation)) {
                return;
            }
            animation.speed.speed = speed;
            // Better Combat re-applies its gear speeds in setupAnim; clear the gears so a paused
            // (0 speed) or explicitly resumed (1 speed) animation keeps the value set here.
            animation.speed.gears = java.util.List.of();
        } catch (Throwable ignored) {
            // Optional compatibility only.
        }
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Search the superclass.
            }
        }
        return null;
    }
}
