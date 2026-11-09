package com.zszq.neonarsenal.weapon;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

/**
 * Immutable description of one arsenal weapon.
 *
 * <p>This is the single place where gameplay-relevant data for a weapon lives. Registration
 * ({@link com.zszq.neonarsenal.registry.ModItems}), creative tabs, models, translations,
 * recipes and the Better Combat integration are all derived from (or documented against) an
 * instance of this record. Adding a weapon therefore never requires touching the registration
 * plumbing: append a definition to {@link WeaponCatalog} and add the matching resources.</p>
 *
 * <p>The record deliberately knows nothing about Better Combat besides an opaque preset id. The
 * concrete weapon attributes are written out per weapon in
 * {@code data/bettercombat_neonarsenal/weapon_attributes/&lt;id&gt;.json}: the definition names the base
 * preset family (used for documentation/logging), while the JSON owns the actual range bonus,
 * two-handed flag, pose, hitboxes, damage multipliers and the Better Combat preset animation ids.
 * This keeps the Java code free of Better Combat classes and lets the mod load (with vanilla combat
 * behaviour) when Better Combat is absent.</p>
 */
public record WeaponDefinition(
        String id,
        Tier tier,
        float attackDamage,
        float attackSpeed,
        ResourceLocation betterCombatPreset,
        Set<Ability> abilities,
        ItemFactory itemFactory) {

    /** Gameplay skills a weapon can have; everything else falls back to plain attacks. */
    public enum Ability {
        /** Long-press falling slash (下落斩击). */
        FALL_SLASH,
        /** Elytra glide slash during an elytra flight, triggered by holding the attack key (鞘翅斩击). */
        ELYTRA_SLASH,
        /** Right-click blocking with the parry pose. */
        BLOCK,
        /** Jump slam during the falling phase of a jump (跳劈). */
        JUMP_ATTACK
    }

    /** Same id constraint Minecraft applies to registry names. */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]+");

    public WeaponDefinition {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Weapon id must match " + ID_PATTERN.pattern() + ", got: " + id);
        }
        Objects.requireNonNull(tier, "tier");
        if (attackDamage < 0.0F) {
            throw new IllegalArgumentException("attackDamage must not be negative, got: " + attackDamage);
        }
        abilities = Set.copyOf(Objects.requireNonNull(abilities, "abilities"));
    }

    /** {@return whether this weapon may perform the given skill}. */
    public boolean can(Ability ability) {
        return abilities.contains(ability);
    }

    /**
     * Creates the item instance. By default this is a regular {@link SwordItem}; a definition may
     * supply a custom {@link ItemFactory} for future weapon classes (bow-like, staff-like, ...)
     * without changing the registry.
     */
    public Item createItem() {
        Item.Properties properties = new Item.Properties()
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed));
        if (itemFactory == null) {
            return new com.zszq.neonarsenal.item.SharpnessWeaponItem(id, tier, properties, abilities);
        }
        return itemFactory.create(tier, properties);
    }

    /**
     * {@return whether this weapon declares a Better Combat preset}.
     *
     * <p>Weapons without a preset simply keep vanilla combat behaviour and need no
     * {@code weapon_attributes} data file. This is useful for utility items that are not melee
     * weapons but are still registered through the same catalogue.</p>
     */
    public boolean hasBetterCombatPreset() {
        return betterCombatPreset != null;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Extension point for weapon items that are not plain swords. */
    @FunctionalInterface
    public interface ItemFactory {
        Item create(Tier tier, Item.Properties properties);
    }

    /** Fluent builder keeping the catalogue declarations readable. */
    public static final class Builder {
        private final String id;
        private Tier tier = Tiers.IRON;
        private float attackDamage = 3.0F;
        private float attackSpeed = -2.4F;
        private ResourceLocation betterCombatPreset;
        private Set<Ability> abilities = Set.of();
        private ItemFactory itemFactory;

        private Builder(String id) {
            this.id = id;
        }

        /** Material tier; also controls durability repair and enchantability. */
        public Builder tier(Tier tier) {
            this.tier = tier;
            return this;
        }

        /**
         * Damage modifier added on top of the tier's attack damage bonus, identical in meaning to
         * the vanilla sword value (vanilla iron sword uses {@code 3}).
         */
        public Builder attackDamage(float attackDamage) {
            this.attackDamage = attackDamage;
            return this;
        }

        /** Attack speed modifier (vanilla swords use {@code -2.4}, i.e. 1.6 attacks/second). */
        public Builder attackSpeed(float attackSpeed) {
            this.attackSpeed = attackSpeed;
            return this;
        }

        /**
         * Base Better Combat preset family used by this weapon (for example
         * {@link NeonArsenalPresets#CLAYMORE}). The matching
         * {@code data/bettercombat_neonarsenal/weapon_attributes/<id>.json} defines the concrete
         * attributes and references this preset family's player animations.
         */
        public Builder betterCombatPreset(ResourceLocation preset) {
            this.betterCombatPreset = preset;
            return this;
        }

        /** Skills this weapon may perform; omit for a plain attack-only weapon. */
        public Builder abilities(Ability... abilities) {
            this.abilities = Set.of(abilities);
            return this;
        }

        /** Replaces the default {@link SwordItem} factory. */
        public Builder itemFactory(ItemFactory itemFactory) {
            this.itemFactory = itemFactory;
            return this;
        }

        public WeaponDefinition build() {
            return new WeaponDefinition(id, tier, attackDamage, attackSpeed, betterCombatPreset,
                    abilities, itemFactory);
        }
    }
}
