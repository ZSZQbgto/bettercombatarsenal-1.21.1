package com.zszq.neonarsenal.weapon;

import java.util.Set;

import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.world.item.ItemStack;

/**
 * Weapons that use the authored main-hand holding animation
 * ({@code animations/weapon_holding_action_from_behind.json}) while idle and walking.
 *
 * <p>The dagger keeps its previous holding pose.</p>
 */
public final class HoldAnimationWeapons {

    private static final Set<String> IDS = Set.of(
            "sword", "claymore", "twinblade", "doubleaxe", "hengdao", "rapier", "spear");

    private HoldAnimationWeapons() {
    }

    /** {@return whether the stack is an arsenal weapon that uses the authored holding animation}. */
    public static boolean isHoldPoseWeapon(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof SharpnessWeaponItem weapon
                && IDS.contains(weapon.weaponId());
    }
}
