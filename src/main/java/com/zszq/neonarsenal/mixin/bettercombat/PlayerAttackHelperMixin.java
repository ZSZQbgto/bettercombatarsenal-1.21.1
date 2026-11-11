package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.compat.AttackHandTracker;
import com.zszq.neonarsenal.compat.JumpAttackHandler;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redirects Better Combat's attack selection while the player is airborne.
 *
 * <p>Better Combat picks the attack from the item's weapon attributes and a combo counter. It has
 * no airborne condition, so this mixin replaces the selection with the claymore's overhead slam for
 * our weapons in the air (see {@link JumpAttackHandler}). The mixin only ever applies when Better
 * Combat is installed (guarded by {@code NeonArsenalMixinPlugin}).</p>
 */
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public class PlayerAttackHelperMixin {

    @Inject(method = "getCurrentAttack", at = @At("RETURN"), remap = false, require = 0)
    private static void bettercombat_neonarsenal$recordAttackHand(Player player, int comboCount,
                                                             CallbackInfoReturnable<Object> callback) {
        AttackHandTracker.record(player, callback.getReturnValue());
    }

    @Inject(method = "getCurrentAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void bettercombat_neonarsenal$jumpAttack(Player player, int comboCount,
                                                       CallbackInfoReturnable<Object> callback) {
        Object forced = JumpAttackHandler.forPlayer(player, comboCount);
        if (forced != null) {
            callback.setReturnValue(forced);
        }
    }
}
