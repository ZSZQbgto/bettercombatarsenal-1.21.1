package com.zszq.neonarsenal.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.zszq.neonarsenal.compat.ChargeThrustState;

import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;

import net.minecraft.client.player.LocalPlayer;

/**
 * Client-side control of the Better Combat spear thrust animation used as the charge-thrust windup.
 *
 * <p>Same mechanism as {@link FallSlamAnimation}: the animation is played manually on Better Combat's
 * attack animation layer with the player's attack cooldown as its length, then frozen at one third of
 * its length while the attack key is held. Releasing resumes the original speed so the whole thrust
 * animation completes, and the pose then fades to idle over 0.3 s.</p>
 */
public final class ChargeThrustAnimation {

    /** Better Combat's two-handed spear stab animation (the spear weapon attributes use the same). */
    public static final String ANIMATION = "bettercombat:two_handed_stab_right";

    private static final float UPSWING = 0.5F;

    private static boolean controlling;
    private static boolean paused;
    private static boolean frozen;
    private static boolean allowStop;

    private static Method cooldownMethod;
    private static boolean cooldownResolved;

    private ChargeThrustAnimation() {
    }

    /** Manually starts the thrust animation and freezes it once it reaches one third. */
    public static void onExternalStart(LocalPlayer player) {
        if (controlling) {
            return;
        }
        controlling = true;
        paused = false;
        frozen = false;
        // Remove the animation that is currently playing without a fade, so the windup rises
        // smoothly from the current pose instead of cross-fading a full swing.
        allowStop = true;
        try {
            ((PlayerAttackAnimatable) player).stopAttackAnimation(0.0F);
        } catch (Throwable ignored) {
            // Better Combat optional.
        } finally {
            allowStop = false;
        }
        ChargeThrustState.allowAnimation = true;
        try {
            float length = cooldownTicks(player);
            ((PlayerAttackAnimatable) player).playAttackAnimation(
                    ANIMATION, AnimatedHand.TWO_HANDED, length, UPSWING);
            paused = true;
            setSpeed(player, 1.0F);
        } catch (Throwable ignored) {
            // Better Combat missing or its internals changed: degrade to a plain attack.
            controlling = false;
            paused = false;
            frozen = false;
        } finally {
            ChargeThrustState.allowAnimation = false;
        }
    }

    /** Called every client tick; freezes the animation once it reaches one third of its length. */
    public static void tickPause(LocalPlayer player) {
        if (!controlling || !paused) {
            return;
        }
        if (frozen) {
            // Keep re-applying the pause: Better Combat recomputes the animation speed modifiers
            // every frame, and any external re-apply must not un-freeze the windup.
            setSpeed(player, 0.0F);
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

    /** Resumes the paused animation at the original speed (1.0). */
    public static void resumeOriginal(LocalPlayer player) {
        if (!controlling) {
            return;
        }
        paused = false;
        frozen = false;
        setSpeed(player, 1.0F);
    }

    /** {@return true once the manual animation reached its final keyframe (or no longer exists)}. */
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
            return tick >= 0 && end > 0 && tick >= Math.max(1, end - 1);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Fades the finished pose to idle over the given number of ticks. */
    public static void finishAndFade(LocalPlayer player, float fadeTicks) {
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

    /** Aborts the charge (released/aborted before completion) with a short fade. */
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

    /** Forgets the ownership state without touching any animation (player replaced). */
    public static void reset() {
        controlling = false;
        paused = false;
        frozen = false;
        allowStop = false;
    }

    /** {@return true while this class owns the attack animation layer}. */
    public static boolean isControlling() {
        return controlling;
    }

    /** {@return true only for this class' own {@code stopAttackAnimation} call}. */
    public static boolean isAllowingStop() {
        return allowStop;
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
