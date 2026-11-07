package com.zszq.neonarsenal.item;

import com.zszq.neonarsenal.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * Sharpness index of an arsenal weapon.
 *
 * <p>Weapons have no durability and never break. They carry a sharpness value from 0 to 3000:
 * 3000 means 100% damage, every 20% lost (600 points) reduces damage by 5% (so 0 sharpness still
 * deals 75%). Every attack hit or broken block costs 1 point. A honeycomb restores 1000 points in
 * an anvil (vanilla material-repair style).</p>
 */
public final class Sharpness {

    public static final int MAX = 3000;
    public static final int PER_HONEYCOMB = 1000;

    private Sharpness() {
    }

    public static int get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MAX;
        }
        return Math.max(0, Math.min(MAX, stack.getOrDefault(ModDataComponents.SHARPNESS.get(), MAX)));
    }

    public static void set(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(ModDataComponents.SHARPNESS.get(), Math.max(0, Math.min(MAX, value)));
    }

    public static void consume(ItemStack stack) {
        set(stack, Math.max(0, get(stack) - 1));
    }

    /** 100% at full sharpness, minus 5% per 20% lost (75% at zero). */
    public static float damageMultiplier(ItemStack stack) {
        return damageMultiplier(get(stack));
    }

    public static float damageMultiplier(int sharpness) {
        int lost = MAX - sharpness;
        int steps = lost / (MAX / 5);
        return Math.max(0.0F, 1.0F - 0.05F * steps);
    }

    /**
     * Blade glow/particle strength: every 10% lost reduces it by 20%; at 50% or more lost (sharpness
     * &lt;= 1500) the weapon neither glows nor emits particles.
     */
    public static float glowFactor(ItemStack stack) {
        return glowFactor(get(stack));
    }

    public static float glowFactor(int sharpness) {
        int lost = MAX - sharpness;
        int steps = (lost * 10) / MAX;
        return Math.max(0.0F, 1.0F - 0.2F * steps);
    }

    /** Honeycombs needed to reach full sharpness. */
    public static int honeycombsNeeded(int sharpness) {
        return (MAX - sharpness + PER_HONEYCOMB - 1) / PER_HONEYCOMB;
    }
}
