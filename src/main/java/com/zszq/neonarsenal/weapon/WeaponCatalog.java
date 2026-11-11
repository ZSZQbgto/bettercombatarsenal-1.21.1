package com.zszq.neonarsenal.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.zszq.neonarsenal.weapon.WeaponDefinition.Ability;

import net.minecraft.world.item.Tiers;

/**
 * The arsenal's weapon catalogue: the one list to extend when a new weapon is added.
 *
 * <p>Every entry is registered as {@code bettercombat_neonarsenal:<id>}. Keep the id in sync with:</p>
 * <ul>
 *   <li>{@code assets/bettercombat_neonarsenal/models/item/<id>.json}</li>
 *   <li>{@code assets/bettercombat_neonarsenal/textures/...} referenced by that model</li>
 *   <li>{@code data/bettercombat_neonarsenal/weapon_attributes/<id>.json} (Better Combat binding)</li>
 *   <li>{@code assets/bettercombat_neonarsenal/lang/*.json} (item name)</li>
 *   <li>recipe and {@code data/minecraft/tags/item/swords.json} entries (if applicable)</li>
 * </ul>
 *
 * <p>Weapon skills are declared per entry: falling slash, blocking and jump attack. Weapons without
 * an ability simply keep plain attacks for that input.</p>
 */
public final class WeaponCatalog {

    /**
     * Player base attack damage (1.0) plus the Iron tier attack damage bonus (2.0). The item tooltip
     * shows {@code attackDamage + TOOLTIP_BASE}, so the catalogue below expresses the intended final
     * base damage through {@link #baseDamage(float)}.
     */
    private static final float TOOLTIP_BASE = 1.0F + Tiers.IRON.getAttackDamageBonus();

    private static final List<WeaponDefinition> WEAPONS = List.of(
            WeaponDefinition.builder("sword")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(5.3F))
                    .attackSpeed(-2.4F)
                    .betterCombatPreset(NeonArsenalPresets.SWORD)
                    .abilities(Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("claymore")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(11.5F))
                    .attackSpeed(-3.2F)
                    .betterCombatPreset(NeonArsenalPresets.CLAYMORE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("doubleaxe")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(15.0F))
                    .attackSpeed(-3.4F)
                    .betterCombatPreset(NeonArsenalPresets.DOUBLE_AXE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("twinblade")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(6.6F))
                    .attackSpeed(-2.6F)
                    .betterCombatPreset(NeonArsenalPresets.TWIN_BLADE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("hengdao")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(8.5F))
                    .attackSpeed(-2.9F)
                    .betterCombatPreset(NeonArsenalPresets.KATANA)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("rapier")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(3.7F))
                    .attackSpeed(-1.8F)
                    .betterCombatPreset(NeonArsenalPresets.RAPIER)
                    .abilities(Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("dagger")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(2.9F))
                    .attackSpeed(-0.8F)
                    .betterCombatPreset(NeonArsenalPresets.DAGGER)
                    .build(),
            WeaponDefinition.builder("spear")
                    .tier(Tiers.IRON)
                    .attackDamage(baseDamage(7.5F))
                    .attackSpeed(-2.8F)
                    .betterCombatPreset(NeonArsenalPresets.SPEAR)
                    .abilities(Ability.CHARGE_THRUST, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build());

    private static final Map<String, WeaponDefinition> BY_ID;

    static {
        Map<String, WeaponDefinition> byId = new LinkedHashMap<>();
        for (WeaponDefinition weapon : WEAPONS) {
            if (byId.put(weapon.id(), weapon) != null) {
                throw new IllegalStateException("Duplicate arsenal weapon id: " + weapon.id());
            }
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    private WeaponCatalog() {
    }

    /** {@return the item attack-damage modifier that shows up as the given tooltip base damage}. */
    private static float baseDamage(float tooltipDamage) {
        return tooltipDamage - TOOLTIP_BASE;
    }

    /** {@return all weapons in display/registration order}. */
    public static List<WeaponDefinition> all() {
        return WEAPONS;
    }

    public static Optional<WeaponDefinition> find(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
