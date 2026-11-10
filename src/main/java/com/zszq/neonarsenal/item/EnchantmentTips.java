package com.zszq.neonarsenal.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zszq.neonarsenal.weapon.WeaponEnchantments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;

/**
 * Tooltip computation for the vanilla enchantments of the arsenal weapons.
 *
 * <p>The tooltip only lists the enchantment <b>names</b>; their numeric effect is folded into the
 * combined percentage lines of the total stats: damage-type enchantments (sharpness, smite, bane of
 * arthropods, fire aspect) are summed into the damage percentage and enchantment attribute modifiers
 * (for example a data-driven attack speed enchantment) are added to the attack speed percentage.
 * Both percentages are relative to the weapon's displayed base attack damage / vanilla base attack
 * speed, the same denominator as the forge bonus lines.</p>
 */
final class EnchantmentTips {

    private static final String PREFIX = "tooltip.bettercombat_neonarsenal.";

    /** Display order: the damage family first, then the remaining weapon enchantments. */
    private static final List<String> ORDER = List.of(
            "sharpness", "smite", "bane_of_arthropods", "sweeping_edge", "fire_aspect",
            "knockback", "looting", "unbreaking", "mending");

    /** Vanilla player base attack speed; used to turn absolute attack speed modifiers into a %. */
    private static final double BASE_ATTACK_SPEED = 4.0D;

    private EnchantmentTips() {
    }

    /** {@return the enchantments of the stack in a stable display order}. */
    static List<Map.Entry<Holder<Enchantment>, Integer>> enchantments(ItemStack stack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        List<Map.Entry<Holder<Enchantment>, Integer>> entries = new ArrayList<>(enchantments.size());
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            entries.add(entry);
        }
        entries.sort((left, right) -> Integer.compare(rank(left.getKey()), rank(right.getKey())));
        return entries;
    }

    /** {@return the enchantment names only, in display order (their numbers are folded into the
     *  combined stats lines)}. */
    static List<Component> names(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            lines.add(Enchantment.getFullname(entry.getKey(), entry.getValue())
                    .copy().withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /**
     * {@return the combined damage bonus of all enchantments as a fraction of the weapon's base
     * attack damage (0 when the weapon has no damage enchantment)}.
     *
     * <p>Sharpness, smite, bane of arthropods and fire aspect are all converted with the same
     * vanilla damage values the per-enchantment lines used before: sharpness {@code 1 + 0.5*(level-1)}
     * damage, smite/bane {@code 2.5*level}, fire aspect {@code 4*level} burn damage.</p>
     */
    static double damageFraction(ItemStack stack) {
        double damage = 0.0D;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getValue();
            if (is(enchantment, "sharpness")) {
                damage += level <= 1 ? 1.0D : 1.0D + 0.5D * (level - 1);
            } else if (is(enchantment, "smite") || is(enchantment, "bane_of_arthropods")) {
                damage += 2.5D * level;
            } else if (is(enchantment, "fire_aspect")) {
                damage += 4.0D * level;
            }
        }
        return damage / baseAttackDamage(stack);
    }

    /**
     * {@return the attack speed bonus added by enchantment attribute modifiers as a fraction of the
     * vanilla base attack speed (0 when no enchantment modifies attack speed)}.
     *
     * <p>Vanilla 1.21 enchantments are data driven, so a pack can add attack speed through
     * {@code EnchantmentEffectComponents.ATTRIBUTES}; absolute values are divided by the vanilla base
     * attack speed, relative values are used as the fraction directly.</p>
     */
    static double attackSpeedFraction(ItemStack stack) {
        double bonus = 0.0D;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            for (EnchantmentAttributeEffect effect : entry.getKey().value()
                    .getEffects(EnchantmentEffectComponents.ATTRIBUTES)) {
                if (!effect.attribute().is(Attributes.ATTACK_SPEED)) {
                    continue;
                }
                double amount = effect.amount().calculate(entry.getValue());
                bonus += switch (effect.operation()) {
                    case ADD_VALUE -> amount / BASE_ATTACK_SPEED;
                    case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> amount;
                };
            }
        }
        return bonus;
    }

    /** {@return the displayed attack damage of the weapon: player base 1 + the item's modifiers}. */
    private static double baseAttackDamage(ItemStack stack) {
        double damage = 1.0D;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.slot() == EquipmentSlotGroup.MAINHAND
                    && entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage += entry.modifier().amount();
            }
        }
        return Math.max(1.0D, damage);
    }

    private static boolean is(Holder<Enchantment> enchantment, String path) {
        ResourceLocation id = WeaponEnchantments.idOf(enchantment);
        return id != null && "minecraft".equals(id.getNamespace()) && path.equals(id.getPath());
    }

    private static int rank(Holder<Enchantment> enchantment) {
        ResourceLocation id = WeaponEnchantments.idOf(enchantment);
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return ORDER.size();
        }
        int index = ORDER.indexOf(id.getPath());
        return index < 0 ? ORDER.size() : index;
    }
}
