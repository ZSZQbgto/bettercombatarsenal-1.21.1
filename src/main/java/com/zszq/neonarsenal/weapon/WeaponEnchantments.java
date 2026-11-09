package com.zszq.neonarsenal.weapon;

import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Per-weapon vanilla enchantment applicability for the arsenal weapons.
 *
 * <p>This mirrors the vanilla allowed lists, including the vanilla mutual exclusions (which stay in
 * the vanilla {@code exclusive_set} tags, so they keep working for the enchanting table, the anvil
 * and the {@code /enchant} command). It replaces the vanilla item tags: the arsenal weapons are not
 * vanilla swords/axes, so they need their own whitelist instead of being added to
 * {@code #minecraft:enchantable/sword}, which would also drag in sweeping edge for the axe-like
 * weapons.</p>
 *
 * <p>Two levels are modelled:</p>
 * <ul>
 *   <li>{@link #supports(String, Holder)} - natural applicability (enchanting table, anvil with a
 *       book). Only the listed enchantments pass.</li>
 *   <li>Density, breach and impaling are intentionally absent: like vanilla non-mace/non-trident
 *       weapons they cannot be obtained naturally. The {@code /enchant} command bypasses this
 *       check for arsenal weapons so they can still be forced, and the vanilla exclusive sets then
 *       reject illegal combinations (density/breach vs sharpness/smite/bane/impaling).</li>
 * </ul>
 */
public final class WeaponEnchantments {

    private static final Set<ResourceLocation> BASE = Set.of(
            ResourceLocation.withDefaultNamespace("sharpness"),
            ResourceLocation.withDefaultNamespace("smite"),
            ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
            ResourceLocation.withDefaultNamespace("knockback"),
            ResourceLocation.withDefaultNamespace("looting"),
            ResourceLocation.withDefaultNamespace("fire_aspect"),
            ResourceLocation.withDefaultNamespace("unbreaking"),
            ResourceLocation.withDefaultNamespace("mending"));

    /** Sword-like weapons additionally accept sweeping edge. */
    private static final Set<ResourceLocation> WITH_SWEEPING = Set.of(
            ResourceLocation.withDefaultNamespace("sharpness"),
            ResourceLocation.withDefaultNamespace("smite"),
            ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
            ResourceLocation.withDefaultNamespace("knockback"),
            ResourceLocation.withDefaultNamespace("looting"),
            ResourceLocation.withDefaultNamespace("fire_aspect"),
            ResourceLocation.withDefaultNamespace("unbreaking"),
            ResourceLocation.withDefaultNamespace("mending"),
            ResourceLocation.withDefaultNamespace("sweeping_edge"));

    private static final Map<String, Set<ResourceLocation>> BY_WEAPON = Map.ofEntries(
            Map.entry("sword", WITH_SWEEPING),
            Map.entry("twinblade", WITH_SWEEPING),
            Map.entry("hengdao", WITH_SWEEPING),
            Map.entry("rapier", WITH_SWEEPING),
            Map.entry("dagger", WITH_SWEEPING),
            Map.entry("claymore", BASE),
            Map.entry("doubleaxe", BASE),
            Map.entry("spear", BASE));

    private WeaponEnchantments() {
    }

    /** {@return the enchantments this weapon can naturally receive}. */
    public static Set<ResourceLocation> natural(String weaponId) {
        return BY_WEAPON.getOrDefault(weaponId, BASE);
    }

    /** {@return whether the weapon's natural enchantment list contains the given enchantment}. */
    public static boolean supports(String weaponId, Holder<Enchantment> enchantment) {
        if (weaponId == null || enchantment == null) {
            return false;
        }
        ResourceLocation id = enchantment.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return id != null && natural(weaponId).contains(id);
    }

    /** {@return the resource location of the enchantment, or {@code null} for direct holders}. */
    public static ResourceLocation idOf(Holder<Enchantment> enchantment) {
        return enchantment == null ? null : enchantment.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }
}
