package com.zszq.neonarsenal.item;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Forge layer stat tables.
 *
 * <p>Every material has five levels (the number of copies of that material on the weapon) and its
 * own growth table: slow/heavy echo, mobile chorus, attack-speed creeper and jump-focused rabbit.
 * Every forged layer adds {@code +2%} critical chance. Multiple materials add their bonuses
 * together, and the number of distinct materials multiplies the final total damage (see
 * {@link #typeDamageMultiplier}).</p>
 */
public final class ForgeStats {

    /** Aggregated held bonuses of a weapon's forge layers. */
    public record Stats(double damage, double attackSpeed, double moveSpeed,
                        double jumpHeight, double jumpSlam, double critChance) {
    }

    /** Per level (index 0 = level 1 ... index 4 = level 5). */
    private static final double[] CHORUS_DAMAGE = {0.10D, 0.25D, 0.45D, 0.75D, 1.15D};
    private static final double[] CHORUS_MOVE = {0.02D, 0.04D, 0.06D, 0.08D, 0.10D};
    private static final double[] CREEPER_DAMAGE = {0.05D, 0.12D, 0.20D, 0.30D, 0.40D};
    private static final double[] CREEPER_SPEED = {0.05D, 0.12D, 0.20D, 0.30D, 0.40D};
    private static final double[] RABBIT_SPEED = {0.05D, 0.12D, 0.20D, 0.30D, 0.40D};
    private static final double[] RABBIT_DAMAGE = {0.04D, 0.10D, 0.18D, 0.26D, 0.35D};
    private static final double[] RABBIT_JUMP = {0.30D, 0.60D, 0.90D, 1.20D, 1.50D};
    private static final double[] RABBIT_SLAM = {0.20D, 0.40D, 0.60D, 0.80D, 1.00D};
    private static final double[] ECHO_DAMAGE = {0.30D, 0.60D, 0.90D, 1.20D, 1.50D};
    /** Echo keeps a fixed -20% attack speed at every level; the damage column is its growth table. */
    private static final double ECHO_SPEED = -0.20D;
    /** Every forged layer grants +2% critical chance (up to +12% at six layers). */
    private static final double CRIT_PER_LAYER = 0.02D;
    /**
     * Total-damage multiplier by the number of distinct forged material types on the weapon
     * (index 0 = no layers, index 4 = all four materials).
     */
    private static final double[] TYPE_DAMAGE_MULTIPLIER = {1.00D, 1.05D, 1.15D, 1.30D, 1.50D};

    private ForgeStats() {
    }

    public static Stats of(ItemStack stack) {
        double damage = 0.0D;
        double attackSpeed = 0.0D;
        double moveSpeed = 0.0D;
        double jumpHeight = 0.0D;
        double jumpSlam = 0.0D;
        List<String> layers = Forge.distinctLayers(stack);
        for (String layer : layers) {
            int level = Math.min(Forge.MAX_LEVEL, Forge.level(stack, layer));
            if (level <= 0) {
                continue;
            }
            int index = level - 1;
            switch (layer) {
                case Forge.CHORUS -> {
                    damage += CHORUS_DAMAGE[index];
                    moveSpeed += CHORUS_MOVE[index];
                }
                case Forge.CREEPER -> {
                    damage += CREEPER_DAMAGE[index];
                    attackSpeed += CREEPER_SPEED[index];
                }
                case Forge.RABBIT -> {
                    attackSpeed += RABBIT_SPEED[index];
                    damage += RABBIT_DAMAGE[index];
                    jumpHeight += RABBIT_JUMP[index];
                    jumpSlam += RABBIT_SLAM[index];
                }
                case Forge.ECHO -> {
                    attackSpeed += ECHO_SPEED;
                    damage += ECHO_DAMAGE[index];
                }
                default -> {
                    // Unknown layer.
                }
            }
        }
        return new Stats(damage, attackSpeed, moveSpeed, jumpHeight, jumpSlam, critChance(stack));
    }

    /** {@return the critical-hit chance granted by the number of forged layers}. */
    public static double critChance(ItemStack stack) {
        return Math.min(0.12D, CRIT_PER_LAYER * Forge.layers(stack).size());
    }

    /**
     * {@return the total-damage multiplier granted by the number of distinct forged material types}
     *
     * <p>Mixing different materials rewards the player: 0 types deal normal damage, 1 type deals
     * {@code x1.05}, 2 types {@code x1.15}, 3 types {@code x1.3} and all 4 types {@code x1.5}. The
     * multiplier is applied to the final damage of a hit, so it stacks multiplicatively with the
     * sharpness, critical-hit and jump-slam multipliers.</p>
     */
    public static double typeDamageMultiplier(ItemStack stack) {
        int types = Math.min(Forge.distinctLayers(stack).size(), TYPE_DAMAGE_MULTIPLIER.length - 1);
        return TYPE_DAMAGE_MULTIPLIER[types];
    }

    /**
     * {@return the bonuses granted by one material at the given layer count}; used by the per-layer
     * tooltip line to show the current level instead of the level-1 base values.
     */
    public static Stats forLayer(String layer, int level) {
        int index = Math.max(0, Math.min(Forge.MAX_LEVEL - 1, level - 1));
        double damage = 0.0D;
        double attackSpeed = 0.0D;
        double moveSpeed = 0.0D;
        double jumpHeight = 0.0D;
        double jumpSlam = 0.0D;
        switch (layer) {
            case Forge.CHORUS -> {
                damage = CHORUS_DAMAGE[index];
                moveSpeed = CHORUS_MOVE[index];
            }
            case Forge.CREEPER -> {
                damage = CREEPER_DAMAGE[index];
                attackSpeed = CREEPER_SPEED[index];
            }
            case Forge.RABBIT -> {
                attackSpeed = RABBIT_SPEED[index];
                damage = RABBIT_DAMAGE[index];
                jumpHeight = RABBIT_JUMP[index];
                jumpSlam = RABBIT_SLAM[index];
            }
            case Forge.ECHO -> {
                attackSpeed = ECHO_SPEED;
                damage = ECHO_DAMAGE[index];
            }
            default -> {
                return new Stats(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return new Stats(damage, attackSpeed, moveSpeed, jumpHeight, jumpSlam,
                CRIT_PER_LAYER * Math.max(1, level));
    }
}
