package com.zszq.neonarsenal.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;

/**
 * Tracks which players currently have a Better Combat attack animation running, so the authored
 * holding pose can fade out while a swing plays and fade back in afterwards.
 *
 * <p>Better Combat's attack animation length is the attack cooldown in ticks; the animation is
 * active for that many ticks (plus a small margin for the fade-out), which is a good enough window
 * for a smooth cross-fade.</p>
 */
public final class AttackAnimationTracker {

    private static final Map<UUID, Integer> END_TICK = new HashMap<>();

    private AttackAnimationTracker() {
    }

    /** Records an attack animation that just started on the client. */
    public static void onAnimationStart(Player player, float lengthTicks) {
        if (player == null) {
            return;
        }
        int length = Math.max(1, (int) Math.ceil(lengthTicks));
        END_TICK.put(player.getUUID(), player.tickCount + length + 2);
    }

    /** Clears the animation state when Better Combat stops the attack animation early. */
    public static void onAnimationStop(Player player) {
        if (player != null) {
            END_TICK.remove(player.getUUID());
        }
    }

    /** {@return whether the player is currently playing an attack animation}. */
    public static boolean isAnimating(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUUID();
        Integer end = END_TICK.get(id);
        if (end == null) {
            return false;
        }
        if (player.tickCount > end) {
            END_TICK.remove(id);
            return false;
        }
        return true;
    }
}
