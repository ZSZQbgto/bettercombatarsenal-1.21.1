package com.zszq.neonarsenal.item;

import java.util.Map;

import com.zszq.neonarsenal.registry.ModDataComponents;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Sharpness index of an arsenal weapon.
 *
 * <p>Weapons have no durability and never break. They carry a sharpness value from 0 to 3000:
 * 3000 means 100% damage, every 20% lost (600 points) reduces damage by 5% (so 0 sharpness still
 * deals 75%). Every attack hit or broken block costs 1 point, reduced by Unbreaking exactly like
 * vanilla durability (level L consumes with a chance of 1/(L+1), so the expected loss rate becomes
 * 1/(L+1) of one point per use). A honeycomb restores 1000 points in an anvil (vanilla
 * material-repair style).</p>
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

    /** {@return the vanilla Unbreaking level of the stack, or 0 when the enchantment is absent}. */
    public static int unbreakingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        for (Map.Entry<Holder<Enchantment>, Integer> entry
                : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey()
                    .map(key -> key.location())
                    .orElse(null);
            if (id != null && "minecraft".equals(id.getNamespace()) && "unbreaking".equals(id.getPath())) {
                return Math.max(0, entry.getValue());
            }
        }
        return 0;
    }

    /**
     * Rolls whether one use consumes a sharpness point.
     *
     * <p>Unbreaking follows the vanilla durability formula: with level L the chance to consume is
     * {@code 1/(L+1)} (level I 50%, II 33.3%, III 25%), so the expected sharpness loss rate of a hit
     * or broken block drops from 1 point to {@code 1/(L+1)} points.</p>
     */
    public static boolean rollConsume(ItemStack stack, RandomSource random) {
        int level = unbreakingLevel(stack);
        return level <= 0 || random.nextFloat() < 1.0F / (level + 1.0F);
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
