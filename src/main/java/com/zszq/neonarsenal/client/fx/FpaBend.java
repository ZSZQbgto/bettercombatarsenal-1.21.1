package com.zszq.neonarsenal.client.fx;

import java.lang.reflect.Method;

import net.minecraft.client.model.geom.ModelPart;

/**
 * Optional bridge to FancyPlayerAnimation's bend hooks. Only used while the block pose is active,
 * so normal BC/FPA bends are untouched outside blocking.
 */
public final class FpaBend {

    private static final double VANILLA_UPPER_ARM = 4.0D;
    private static Method setHook;
    private static Method angleHook;
    private static boolean lookedUp;

    private FpaBend() {
    }

    private static void lookup() {
        if (lookedUp) {
            return;
        }
        lookedUp = true;
        try {
            Class<?> hooks = Class.forName("com.superanim.client.BendHooks");
            setHook = hooks.getMethod("set", ModelPart.class, double.class, double.class);
            angleHook = hooks.getMethod("angle", ModelPart.class);
        } catch (Throwable ignored) {
            setHook = null;
            angleHook = null;
        }
    }

    public static boolean available() {
        lookup();
        return setHook != null;
    }

    /** Blends our authored elbow angle over the current bend; weight 1 means blocking wins. */
    public static void blend(ModelPart part, float targetAngle, float weight) {
        if (part == null) {
            return;
        }
        lookup();
        if (setHook == null) {
            return;
        }
        try {
            float current = 0.0F;
            if (angleHook != null) {
                Object value = angleHook.invoke(null, part);
                if (value instanceof Float f) {
                    current = f;
                }
            }
            float angle = current + (targetAngle - current) * Math.max(0.0F, Math.min(1.0F, weight));
            setHook.invoke(null, part, (double) angle, VANILLA_UPPER_ARM);
        } catch (Throwable ignored) {
            // Optional compatibility only.
        }
    }
}
