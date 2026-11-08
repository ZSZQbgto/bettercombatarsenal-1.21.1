package com.zszq.neonarsenal.compat;

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

    /** Jump slam knockback: Knockback V equivalent (0.5 velocity per level). */
    private static final double KNOCKBACK_V = 2.5D;

    private JumpAttackKnockback() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }
        if (!JumpAttackHandler.isJumpAttackAirborne(player)) {
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
        target.knockback(KNOCKBACK_V, player.getX() - target.getX(),
                player.getZ() - target.getZ());
    }
}
