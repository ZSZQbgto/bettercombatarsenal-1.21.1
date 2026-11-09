package com.zszq.neonarsenal.mixin.bettercombat;

import com.zszq.neonarsenal.weapon.HoldAnimationWeapons;

import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces Better Combat's authored <b>main-hand</b> pose with the arsenal's own holding animation
 * for the weapons that use it.
 *
 * <p>The offhand pose is kept as it is, so dual wielding still shows the offhand weapon's original
 * pose while the main hand uses the authored holding action (applied by
 * {@link com.zszq.neonarsenal.client.HoldPose}).</p>
 */
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public class PoseForPlayerHoldPoseMixin {

    @Inject(method = "poseForPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private static void neonarsenal$replaceMainHandPose(Player player,
                                                        CallbackInfoReturnable<Pose> callback) {
        Pose pose = callback.getReturnValue();
        if (pose == null || player == null) {
            return;
        }
        if (HoldAnimationWeapons.isHoldPoseWeapon(player.getMainHandItem())) {
            callback.setReturnValue(new Pose("", pose.offHand()));
        }
    }
}
