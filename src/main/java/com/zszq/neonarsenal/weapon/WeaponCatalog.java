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

    private static final List<WeaponDefinition> WEAPONS = List.of(
            WeaponDefinition.builder("sword")
                    .tier(Tiers.IRON)
                    .attackDamage(3.3F)
                    .attackSpeed(-2.4F)
                    .betterCombatPreset(NeonArsenalPresets.SWORD)
                    .abilities(Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("claymore")
                    .tier(Tiers.IRON)
                    .attackDamage(12.0F)
                    .attackSpeed(-3.2F)
                    .betterCombatPreset(NeonArsenalPresets.CLAYMORE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("doubleaxe")
                    .tier(Tiers.IRON)
                    .attackDamage(17.0F)
                    .attackSpeed(-3.4F)
                    .betterCombatPreset(NeonArsenalPresets.DOUBLE_AXE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("twinblade")
                    .tier(Tiers.IRON)
                    .attackDamage(6.2F)
                    .attackSpeed(-2.6F)
                    .betterCombatPreset(NeonArsenalPresets.TWIN_BLADE)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("hengdao")
                    .tier(Tiers.IRON)
                    .attackDamage(8.0F)
                    .attackSpeed(-2.9F)
                    .betterCombatPreset(NeonArsenalPresets.KATANA)
                    .abilities(Ability.FALL_SLASH, Ability.ELYTRA_SLASH, Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("rapier")
                    .tier(Tiers.IRON)
                    .attackDamage(2.0F)
                    .attackSpeed(-1.8F)
                    .betterCombatPreset(NeonArsenalPresets.RAPIER)
                    .abilities(Ability.BLOCK, Ability.JUMP_ATTACK)
                    .build(),
            WeaponDefinition.builder("dagger")
                    .tier(Tiers.IRON)
                    .attackDamage(1.0F)
                    .attackSpeed(-0.8F)
                    .betterCombatPreset(NeonArsenalPresets.DAGGER)
                    .build(),
            WeaponDefinition.builder("spear")
                    .tier(Tiers.IRON)
                    .attackDamage(6.2F)
                    .attackSpeed(-2.8F)
                    .betterCombatPreset(NeonArsenalPresets.SPEAR)
                    .abilities(Ability.BLOCK, Ability.JUMP_ATTACK)
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

    /** {@return all weapons in display/registration order}. */
    public static List<WeaponDefinition> all() {
        return WEAPONS;
    }

    public static Optional<WeaponDefinition> find(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
