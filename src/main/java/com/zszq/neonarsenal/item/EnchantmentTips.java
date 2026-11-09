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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Tooltip computation for the vanilla enchantments of the arsenal weapons.
 *
 * <p>Damage-type enchantments are shown as percentages of the weapon's displayed attack damage, the
 * same {@code +N%} style as the forge bonus lines: sharpness damage, the conditional smite/bane of
 * arthropods damage and the fire aspect burn damage. Unbreaking and sweeping edge are already
 * percentages. Enchantments without a meaningful denominator keep their value text (knockback
 * strength, looting level, mending).</p>
 */
final class EnchantmentTips {

    private static final String PREFIX = "tooltip.bettercombat_neonarsenal.";

    /** Display order: the damage family first, then the remaining weapon enchantments. */
    private static final List<String> ORDER = List.of(
            "sharpness", "smite", "bane_of_arthropods", "sweeping_edge", "fire_aspect",
            "knockback", "looting", "unbreaking", "mending");

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

    /** {@return the damage bonus of sharpness in percent of the base attack damage (0 when absent)}. */
    static double sharpnessPercent(ItemStack stack) {
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            if (is(entry.getKey(), "sharpness")) {
                return percent(entry.getValue() <= 1 ? 1.0D : 1.0D + 0.5D * (entry.getValue() - 1), stack);
            }
        }
        return 0.0D;
    }

    /** {@return the compact numeric bonus lines (always shown in the tooltip)}. */
    static List<Component> bonuses(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            // Sharpness is merged into the total damage percentage line.
            if (is(entry.getKey(), "sharpness")) {
                continue;
            }
            Component bonus = bonusOf(entry.getKey(), entry.getValue(), stack);
            if (bonus != null) {
                lines.add(bonus);
            }
        }
        return lines;
    }

    /** {@return the detailed lines ("name: bonus") shown while Shift is held}. */
    static List<Component> details(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments(stack)) {
            Component name = Enchantment.getFullname(entry.getKey(), entry.getValue());
            Component bonus = bonusOf(entry.getKey(), entry.getValue(), stack);
            if (bonus == null) {
                lines.add(name);
            } else {
                lines.add(Component.empty()
                        .append(name)
                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(bonus));
            }
        }
        return lines;
    }

    /** {@return the numeric effect text for one enchantment, or {@code null} when it has none}. */
    private static Component bonusOf(Holder<Enchantment> enchantment, int level, ItemStack stack) {
        if (level <= 0) {
            return null;
        }
        ResourceLocation id = WeaponEnchantments.idOf(enchantment);
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return null;
        }
        return switch (id.getPath()) {
            // Vanilla 1.21 sharpness: +1 damage for the first level, +0.5 for every level above.
            case "sharpness" -> percentLine("enchant.attack_damage",
                    percent(1.0D + 0.5D * (level - 1), stack), ChatFormatting.GREEN);
            case "smite" -> percentLine("enchant.vs_undead",
                    percent(2.5D * level, stack), ChatFormatting.GREEN);
            case "bane_of_arthropods" -> percentLine("enchant.vs_arthropods",
                    percent(2.5D * level, stack), ChatFormatting.GREEN);
            // Fire aspect keeps the target burning for 4 s per level (about 1 damage per second).
            case "fire_aspect" -> percentLine("enchant.fire_damage",
                    percent(4.0D * level, stack), ChatFormatting.GOLD);
            case "knockback" -> Component.translatable(PREFIX + "enchant.knockback",
                    number(level)).withStyle(ChatFormatting.GREEN);
            case "looting" -> Component.translatable(PREFIX + "enchant.looting",
                    number(level)).withStyle(ChatFormatting.GREEN);
            case "sweeping_edge" -> Component.translatable(PREFIX + "enchant.sweeping",
                    number(Math.round(100.0D * level / (level + 1.0D)))).withStyle(ChatFormatting.GREEN);
            case "unbreaking" -> Component.translatable(PREFIX + "enchant.unbreaking",
                    number(Math.round(100.0D * level / (level + 1.0D)))).withStyle(ChatFormatting.GREEN);
            case "mending" -> Component.translatable(PREFIX + "enchant.mending")
                    .withStyle(ChatFormatting.GREEN);
            default -> null;
        };
    }

    /** {@return {@code damage} as a rounded integer percentage of the weapon's base attack damage}. */
    private static long percent(double damage, ItemStack stack) {
        double base = baseAttackDamage(stack);
        return Math.round(100.0D * damage / base);
    }

    private static Component percentLine(String key, long percent, ChatFormatting colour) {
        return Component.translatable(PREFIX + key, "+" + percent + "%").withStyle(colour);
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

    /** Formats a number without a trailing {@code .0}. */
    private static String number(long value) {
        return Long.toString(value);
    }
}
