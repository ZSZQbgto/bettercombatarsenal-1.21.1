package com.zszq.neonarsenal.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks which hand Better Combat actually used for the current attack (recorded from its own
 * server-side attack selection), so damage/forge/sharpness calculations can use the weapon of the
 * hand that actually attacked.
 *
 * <p>Better Combat temporarily swaps the hand item stacks while it performs an off-hand attack, so
 * during such a hit the off-hand slot is the authoritative source of the attacking weapon; see
 * {@link #attackingWeapon(Player)}. Non-Better-Combat skill damage (falling slash, glide slash,
 * charge thrust) clears the record first, so those always settle with the main-hand weapon.</p>
 *
 * <p>Without Better Combat the tracker stays empty and hits are treated as main-hand hits, which
 * matches vanilla behaviour.</p>
 */
public final class AttackHandTracker {

    private static final Map<UUID, String> LAST_HAND = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();
    private static final long VALID_TICKS = 5L;

    private AttackHandTracker() {
    }

    /** Called from the Better Combat {@code getCurrentAttack} mixin (server side). */
    public static void record(Player player, Object hand) {
        if (hand == null || player.level().isClientSide()) {
            return;
        }
        String name = hand instanceof Enum<?> enumeration ? enumeration.name() : hand.toString();
        LAST_HAND.put(player.getUUID(), name);
        LAST_TICK.put(player.getUUID(), player.level().getGameTime());
    }

    /** Forgets the last recorded hand of a player. */
    public static void clear(UUID id) {
        LAST_HAND.remove(id);
        LAST_TICK.remove(id);
    }

    /** True when the most recent Better Combat attack was performed with the off hand. */
    public static boolean isOffHandAttack(Player player) {
        String hand = LAST_HAND.get(player.getUUID());
        Long tick = LAST_TICK.get(player.getUUID());
        if (hand == null || tick == null) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now - tick > VALID_TICKS) {
            // Lazy expiry: never keep stale entries for players that stopped attacking.
            clear(player.getUUID());
            return false;
        }
        return hand.contains("OFF_HAND");
    }

    /**
     * {@return the weapon that dealt the current hit}
     *
     * <p>During a Better Combat off-hand attack the main-hand stack is temporarily replaced with the
     * off-hand one, while the off-hand slot still holds the weapon that is actually swinging; every
     * other hit is settled with the main-hand weapon.</p>
     */
    public static ItemStack attackingWeapon(Player player) {
        return isOffHandAttack(player) ? player.getOffhandItem() : player.getMainHandItem();
    }
}
