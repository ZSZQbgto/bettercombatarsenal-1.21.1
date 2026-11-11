package com.zszq.neonarsenal.compat;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * One place that drops all server-side per-player state when a player disconnects, so no stale
 * pending slam, jump window or sharpness bookkeeping survives a reconnect.
 */
public final class PlayerStateCleanup {

    private PlayerStateCleanup() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUUID();
        SharpnessEvents.clear(id);
        AttackHandTracker.clear(id);
        FallSlamHandler.clear(id);
        JumpAttackHandler.clear(id);
        BlockHandler.clear(id);
        ForgeEffects.clear(id);
    }
}
