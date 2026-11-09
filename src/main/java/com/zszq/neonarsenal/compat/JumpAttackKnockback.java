package com.zszq.neonarsenal.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Extra knockback for the falling jump attack.
 *
 * <p>Any jump-slam attack with an arsenal weapon is turned into the claymore slam by
 * {@link JumpAttackHandler}; on a hit, this listener pushes the target away like the vanilla
 * <b>Knockback I</b> enchantment (0.5 velocity). The knockback is tied to the actual jump-slam hit
 * ({@link JumpAttackHandler#isRecentJumpSlam(Player)}), so normal attacks made while airborne never
 * get it.</p>
 */
public final class JumpAttackKnockback {

    /** Jump slam knockback: Knockback I equivalent (0.5 velocity per level). */
    private static final double KNOCKBACK_STRENGTH = 0.5D;

    private JumpAttackKnockback() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) {
            return;
        }
        if (!JumpAttackHandler.isRecentJumpSlam(player)) {
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
        target.knockback(KNOCKBACK_STRENGTH, player.getX() - target.getX(),
                player.getZ() - target.getZ());
    }
}
