package com.zszq.bettercombatarsenal.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.Tiers;

/**
 * The arsenal's weapon catalogue: the one list to extend when a new weapon is added.
 *
 * <p>Every entry is registered as {@code bettercombatarsenal:<id>}. Keep the id in sync with:</p>
 * <ul>
 *   <li>{@code assets/bettercombatarsenal/models/item/<id>.json}</li>
 *   <li>{@code assets/bettercombatarsenal/textures/...} referenced by that model</li>
 *   <li>{@code data/bettercombatarsenal/weapon_attributes/<id>.json} (Better Combat binding)</li>
 *   <li>{@code assets/bettercombatarsenal/lang/*.json} (item name)</li>
 *   <li>recipe and {@code data/minecraft/tags/item/swords.json} entries (if applicable)</li>
 * </ul>
 */
public final class WeaponCatalog {

    private static final List<WeaponDefinition> WEAPONS = List.of(
            WeaponDefinition.builder("sword")
                    .tier(Tiers.IRON)
                    .attackDamage(3.0F)
                    .attackSpeed(-2.4F)
                    .betterCombatPreset(BetterCombatPresets.SWORD)
                    .build(),
            WeaponDefinition.builder("claymore")
                    .tier(Tiers.IRON)
                    .attackDamage(5.0F)
                    .attackSpeed(-3.2F)
                    .betterCombatPreset(BetterCombatPresets.CLAYMORE)
                    .build(),
            WeaponDefinition.builder("doubleaxe")
                    .tier(Tiers.IRON)
                    .attackDamage(6.0F)
                    .attackSpeed(-3.4F)
                    .betterCombatPreset(BetterCombatPresets.DOUBLE_AXE)
                    .build(),
            WeaponDefinition.builder("twinblade")
                    .tier(Tiers.IRON)
                    .attackDamage(4.0F)
                    .attackSpeed(-2.8F)
                    .betterCombatPreset(BetterCombatPresets.TWIN_BLADE)
                    .build(),
            WeaponDefinition.builder("hengdao")
                    .tier(Tiers.IRON)
                    .attackDamage(4.5F)
                    .attackSpeed(-3.0F)
                    .betterCombatPreset(BetterCombatPresets.KATANA)
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
