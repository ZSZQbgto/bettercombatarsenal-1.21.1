package com.zszq.bettercombatarsenal.compat;

import java.util.Map;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Extra knockback for the airborne jump attack.
 *
 * <p>Any airborne attack with an arsenal weapon is turned into the claymore slam by
 * {@link JumpAttackHandler}; on a hit, this listener pushes the target away with a strength that
 * depends on the weapon class (light swords less, heavy claymore/double axe more).</p>
 */
public final class JumpAttackKnockback {

    /** Knockback strength per weapon id (in the units of {@link LivingEntity#knockback}). */
    private static final Map<String, Double> STRENGTH = Map.of(
            "sword", 0.35D,
            "twinblade", 0.45D,
            "hengdao", 0.55D,
            "claymore", 0.85D,
            "doubleaxe", 1.0D);

    private JumpAttackKnockback() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }
        if (player.onGround()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        String weaponId = JumpAttackHandler.arsenalId(weapon);
        if (weaponId == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == null || target == player) {
            return;
        }
        double strength = STRENGTH.getOrDefault(weaponId, 0.5D);
        if (strength <= 0.0D) {
            return;
        }
        target.knockback(strength, player.getX() - target.getX(), player.getZ() - target.getZ());
    }
}
