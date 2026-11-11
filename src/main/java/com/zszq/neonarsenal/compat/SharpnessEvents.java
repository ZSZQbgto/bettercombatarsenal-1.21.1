package com.zszq.neonarsenal.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zszq.neonarsenal.item.Sharpness;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Applies the sharpness index to damage, consumption and vanilla-style anvil repair.
 *
 * <p>Damage is scaled by the weapon's own sharpness (during dual wielding each hand uses and consumes
 * the weapon that actually attacked; jump slashes use the main-hand weapon). Each hit costs one
 * point (at most once per player per tick, so one swing hitting several targets still costs one
 * point) and each broken block costs one point; Unbreaking reduces the chance of that loss like
 * vanilla durability (level L consumes with a chance of 1/(L+1)). In an anvil, a weapon plus
 * honeycombs (one honeycomb per 1000 sharpness) restores the weapon like an iron tool repaired with
 * iron ingots; unused honeycombs stay in the anvil.</p>
 */
public final class SharpnessEvents {

    private static final Map<UUID, Integer> LAST_HIT_TICK = new HashMap<>();

    private SharpnessEvents() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            // The target blocked the hit (the blocking listener cancels after this one runs); never
            // consume sharpness or scale damage for a fully blocked hit.
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        boolean offHand = AttackHandTracker.isOffHandAttack(player);
        boolean jumpSlam = JumpAttackHandler.isRecentJumpSlam(player);
        if (jumpSlam && offHand) {
            // Jump slashes always use the main-hand weapon. Better Combat only performs a jump slash
            // with the off hand when the main hand is not an arsenal weapon, so nothing is scaled or
            // consumed for this off-hand hit.
            return;
        }
        ItemStack weapon = AttackHandTracker.attackingWeapon(player);
        if (!(weapon.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        event.setAmount(event.getAmount() * Sharpness.damageMultiplier(weapon));
        Integer lastTick = LAST_HIT_TICK.get(player.getUUID());
        if (lastTick == null || lastTick != player.tickCount) {
            LAST_HIT_TICK.put(player.getUUID(), player.tickCount);
            if (Sharpness.rollConsume(weapon, player.getRandom())) {
                Sharpness.consume(weapon);
            }
        }
    }

    /** Forgets the last-hit tick of a player that left the server. */
    public static void clear(UUID id) {
        LAST_HIT_TICK.remove(id);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.getItem() instanceof SharpnessWeaponItem
                && Sharpness.rollConsume(weapon, player.getRandom())) {
            Sharpness.consume(weapon);
        }
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!(left.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        ItemStack right = event.getRight();
        if (!right.is(Items.HONEYCOMB)) {
            return;
        }
        int sharpness = Sharpness.get(left);
        if (sharpness >= Sharpness.MAX) {
            return;
        }
        int needed = Sharpness.honeycombsNeeded(sharpness);
        int used = Math.min(needed, right.getCount());
        ItemStack output = left.copy();
        Sharpness.set(output, Math.min(Sharpness.MAX, sharpness + used * Sharpness.PER_HONEYCOMB));
        event.setOutput(output);
        // Sharpening in the anvil is free: no experience levels are charged, only the honeycombs.
        event.setCost(0);
        event.setMaterialCost(used);
    }
}
