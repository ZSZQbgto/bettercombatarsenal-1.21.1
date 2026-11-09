package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.item.Sharpness;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Mending on an arsenal weapon restores its sharpness instead of durability.
 *
 * <p>The arsenal weapons are unbreakable, so vanilla Mending has nothing to repair. Every experience
 * point picked up while a Mending weapon is held converts to sharpness: one experience point
 * restores {@link #SHARPNESS_PER_XP} sharpness, consuming that experience point (like vanilla Mending
 * consumes experience to repair). The main hand is repaired first, then the off hand; when both are
 * full the experience is gained normally, and any experience beyond the needed amount is still given
 * to the player (and can repair other Mending gear through vanilla).</p>
 */
public final class MendingSharpness {

    /** Sharpness restored per experience point consumed. */
    public static final int SHARPNESS_PER_XP = 20;

    private MendingSharpness() {
    }

    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ExperienceOrb orb = event.getOrb();
        int value = orb.value;
        if (value <= 0) {
            return;
        }
        ItemStack weapon = mendingWeapon(player);
        if (weapon == null) {
            return;
        }
        int missing = Sharpness.MAX - Sharpness.get(weapon);
        if (missing <= 0) {
            return;
        }
        int xpNeeded = (missing + SHARPNESS_PER_XP - 1) / SHARPNESS_PER_XP;
        int xpUsed = Math.min(value, xpNeeded);
        int restored = Math.min(missing, xpUsed * SHARPNESS_PER_XP);
        Sharpness.set(weapon, Sharpness.get(weapon) + restored);
        // Consume the used experience: the orb's remaining value is what vanilla gives to the player.
        orb.value = value - xpUsed;
    }

    /** {@return the first held Mending arsenal weapon that is missing sharpness, or null}. */
    private static ItemStack mendingWeapon(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (canRepair(main)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (canRepair(off)) {
            return off;
        }
        return null;
    }

    private static boolean canRepair(ItemStack stack) {
        if (!(stack.getItem() instanceof SharpnessWeaponItem)) {
            return false;
        }
        if (Sharpness.get(stack) >= Sharpness.MAX) {
            return false;
        }
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.REPAIR_WITH_XP);
    }
}
