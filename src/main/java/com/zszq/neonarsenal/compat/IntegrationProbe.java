package com.zszq.neonarsenal.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.zszq.neonarsenal.NeonArsenal;

/**
 * Startup probe for the optional Better Combat / Player Animator Library internals this mod accesses
 * reflectively or injects into. A single WARN lists everything that is missing or renamed, so a
 * version update degrades loudly once instead of failing silently in the middle of combat.
 *
 * <p>Client-only (it probes client classes); call from the client setup event.</p>
 */
public final class IntegrationProbe {

    private IntegrationProbe() {
    }

    public static void runClient() {
        List<String> missing = new ArrayList<>();
        int checks = 0;

        checks += checkClass(missing, "net.bettercombat.client.animation.AttackAnimationSubStack");
        checks += checkClass(missing, "net.bettercombat.client.animation.PlayerAttackAnimatable");
        checks += checkClass(missing, "net.bettercombat.api.MinecraftClient_BetterCombat");
        checks += checkClass(missing, "net.bettercombat.client.particle.SlashParticleUtil");
        checks += checkClass(missing, "dev.kosmx.playerAnim.core.impl.AnimationProcessor");
        checks += checkMethod(missing, "net.minecraft.client.player.AbstractClientPlayer",
                "playAttackAnimation");
        checks += checkMethod(missing, "net.minecraft.client.player.AbstractClientPlayer",
                "stopAttackAnimation");
        checks += checkField(missing, "net.minecraft.client.player.AbstractClientPlayer",
                "attackAnimation");
        checks += checkField(missing, "net.minecraft.client.Minecraft", "ongoingSwing");
        checks += checkField(missing, "net.minecraft.client.Minecraft", "upswingStack");
        checks += checkMethod(missing, "net.bettercombat.api.MinecraftClient_BetterCombat",
                "isWeaponSwingInProgress");

        if (missing.isEmpty()) {
            NeonArsenal.LOGGER.debug("Better Combat integration probe OK ({} checks)", checks);
        } else {
            NeonArsenal.LOGGER.warn(
                    "Better Combat integration probe: {} internal(s) missing or renamed, related features "
                            + "degrade gracefully: {}",
                    missing.size(), missing);
        }
    }

    private static int checkClass(List<String> missing, String className) {
        try {
            Class.forName(className, false, IntegrationProbe.class.getClassLoader());
            return 1;
        } catch (Throwable ignored) {
            missing.add(className);
            return 1;
        }
    }

    private static int checkMethod(List<String> missing, String className, String methodName) {
        try {
            Class<?> type = Class.forName(className, false, IntegrationProbe.class.getClassLoader());
            for (Method method : type.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return 1;
                }
            }
            missing.add(className + "#" + methodName);
        } catch (Throwable ignored) {
            missing.add(className + "#" + methodName);
        }
        return 1;
    }

    private static int checkField(List<String> missing, String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className, false, IntegrationProbe.class.getClassLoader());
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(fieldName);
                    if (field != null) {
                        return 1;
                    }
                } catch (NoSuchFieldException ignored) {
                    // Continue with the superclass.
                }
            }
            missing.add(className + "#" + fieldName);
        } catch (Throwable ignored) {
            missing.add(className + "#" + fieldName);
        }
        return 1;
    }
}
