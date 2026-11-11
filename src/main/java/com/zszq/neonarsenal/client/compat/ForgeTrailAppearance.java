package com.zszq.neonarsenal.client.compat;

import java.lang.reflect.Constructor;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.item.Forge;

import net.minecraft.world.item.ItemStack;

/**
 * Builds Better Combat {@code TrailAppearance} objects with a forged colour, without a compile-time
 * Better Combat dependency (reflection).
 */
public final class ForgeTrailAppearance {

    private static boolean initTried;
    private static Constructor<?> appearanceConstructor;
    private static Constructor<?> partConstructor;
    private static boolean logged;
    private static boolean failureLogged;

    private ForgeTrailAppearance() {
    }

    /** Builds a Better Combat trail appearance with the weapon's mixed forge colour. */
    public static Object of(ItemStack stack) {
        init();
        if (appearanceConstructor == null || partConstructor == null) {
            return null;
        }
        try {
            int mixed = Forge.displayColor(stack);
            // Better Combat packs trail colours as RGBA (0xRRGGBBAA).
            long primary = ((long) (mixed & 0xFFFFFF) << 8) | 0xFFL;
            long secondary = ((long) (mixed & 0xFFFFFF) << 8) | 0x99L;
            Object primaryPart = partConstructor.newInstance(primary, true);
            Object secondaryPart = partConstructor.newInstance(secondary, true);
            Object appearance = appearanceConstructor.newInstance(primaryPart, secondaryPart);
            if (!logged) {
                logged = true;
                NeonArsenal.LOGGER.debug(
                        "Forge colours applied to Better Combat's slash trail (scrolling gradient)");
            }
            return appearance;
        } catch (Throwable throwable) {
            warnFailure(throwable);
            return null;
        }
    }

    private static synchronized void init() {
        if (initTried) {
            return;
        }
        initTried = true;
        try {
            Class<?> appearance = Class.forName("net.bettercombat.api.fx.TrailAppearance");
            Class<?> part = Class.forName("net.bettercombat.api.fx.TrailAppearance$Part");
            appearanceConstructor = appearance.getConstructor(part, part);
            partConstructor = part.getConstructor(long.class, boolean.class);
        } catch (Throwable throwable) {
            appearanceConstructor = null;
            partConstructor = null;
            warnFailure(throwable);
        }
    }

    /** Reports the first failure only, so a missing/changed Better Combat API does not spam logs. */
    private static void warnFailure(Throwable throwable) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        NeonArsenal.LOGGER.warn(
                "Better Combat trail appearance is unavailable; forged slash-trail colours are disabled",
                throwable);
    }
}
