package com.zszq.neonarsenal.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.minecraft.client.player.LocalPlayer;

/**
 * Cached reflective access to Better Combat's client attack-animation substack.
 *
 * <p>The fall-slam and charge-thrust controllers need the animation's current/end tick, its active
 * flag, the playback speed and the weapon cooldown every client tick. Resolving the field/method
 * objects once and reusing them keeps the tick path allocation-free (the previous implementation did
 * a {@code getMethod}/{@code findField} lookup on every call).</p>
 */
final class BcAnimationAccess {

    private static Field attackAnimationField;
    private static boolean fieldResolved;
    private static Method currentTickMethod;
    private static Method getDataMethod;
    private static Field endTickField;
    private static Method isActiveMethod;
    private static Method cooldownMethod;
    private static boolean cooldownResolved;
    private static boolean isActiveFailed;

    private BcAnimationAccess() {
    }

    static AttackAnimationSubStack substack(LocalPlayer player) {
        if (player == null) {
            return null;
        }
        if (!fieldResolved) {
            fieldResolved = true;
            attackAnimationField = findField(player.getClass(), "attackAnimation");
            if (attackAnimationField != null) {
                attackAnimationField.setAccessible(true);
            }
        }
        if (attackAnimationField == null) {
            return null;
        }
        try {
            Object value = attackAnimationField.get(player);
            return value instanceof AttackAnimationSubStack stack ? stack : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean hasAnimation(LocalPlayer player) {
        AttackAnimationSubStack stack = substack(player);
        return stack != null && stack.base != null && stack.base.getAnimation() != null;
    }

    static int currentTick(LocalPlayer player) {
        Object animation = animation(player);
        if (animation == null) {
            return -1;
        }
        if (currentTickMethod == null) {
            currentTickMethod = findMethod(animation.getClass(), "getCurrentTick");
        }
        if (currentTickMethod == null) {
            return -1;
        }
        try {
            return ((Number) currentTickMethod.invoke(animation)).intValue();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    static int endTick(LocalPlayer player) {
        Object animation = animation(player);
        if (animation == null) {
            return -1;
        }
        if (getDataMethod == null) {
            getDataMethod = findMethod(animation.getClass(), "getData");
        }
        if (getDataMethod == null) {
            return -1;
        }
        try {
            Object data = getDataMethod.invoke(animation);
            if (endTickField == null) {
                endTickField = data.getClass().getField("endTick");
            }
            return endTickField.getInt(data);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** {@return the animation's active flag, or {@code null} when it cannot be inspected}. */
    static Boolean isActive(LocalPlayer player) {
        Object animation = animation(player);
        if (animation == null || isActiveFailed) {
            return null;
        }
        if (isActiveMethod == null) {
            isActiveMethod = findMethod(animation.getClass(), "isActive");
            if (isActiveMethod == null) {
                isActiveFailed = true;
                return null;
            }
        }
        try {
            return (Boolean) isActiveMethod.invoke(animation);
        } catch (Throwable ignored) {
            isActiveFailed = true;
            return null;
        }
    }

    static void setSpeed(LocalPlayer player, float speed) {
        AttackAnimationSubStack stack = substack(player);
        if (stack == null) {
            return;
        }
        try {
            stack.speed.speed = speed;
            // Better Combat re-applies its gear speeds in setupAnim; clear the gears so a paused
            // (0 speed) or explicitly resumed (1 speed) animation keeps the value set here.
            stack.speed.gears = java.util.List.of();
        } catch (Throwable ignored) {
            // Optional compatibility only.
        }
    }

    /** {@return Better Combat's attack cooldown in ticks (capped), or 20 when unavailable}. */
    static float cooldownTicks(LocalPlayer player) {
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
        if (cooldownMethod != null && player != null) {
            try {
                return Math.max(1.0F, ((Number) cooldownMethod.invoke(null, player)).floatValue());
            } catch (Throwable ignored) {
                // Fall through to the default cooldown.
            }
        }
        return 20.0F;
    }

    private static Object animation(LocalPlayer player) {
        AttackAnimationSubStack stack = substack(player);
        return stack == null || stack.base == null ? null : stack.base.getAnimation();
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Search the superclass.
            }
        }
        return null;
    }
}
