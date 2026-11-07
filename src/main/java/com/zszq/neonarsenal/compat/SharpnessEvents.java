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
 * <p>Damage is scaled by the weapon sharpness. Each hit costs one point (at most once per player
 * per tick, so one swing hitting several targets still costs one point) and each broken block costs
 * one point. In an anvil, a weapon plus honeycombs (one honeycomb per 1000 sharpness) restores the
 * weapon like an iron tool repaired with iron ingots; unused honeycombs stay in the anvil.</p>
 */
public final class SharpnessEvents {

    private static final Map<UUID, Integer> LAST_HIT_TICK = new HashMap<>();

    private SharpnessEvents() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        if (AttackHandTracker.isOffHandAttack(player)) {
            // Dual wielding: off-hand hits neither scale nor consume the main-hand sharpness.
            return;
        }
        event.setAmount(event.getAmount() * Sharpness.damageMultiplier(weapon));
        Integer lastTick = LAST_HIT_TICK.get(player.getUUID());
        if (lastTick == null || lastTick != player.tickCount) {
            LAST_HIT_TICK.put(player.getUUID(), player.tickCount);
            Sharpness.consume(weapon);
        }
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
        if (weapon.getItem() instanceof SharpnessWeaponItem) {
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
