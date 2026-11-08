package com.zszq.neonarsenal.weapon;

import net.minecraft.resources.ResourceLocation;

/**
 * Ids of the Better Combat 2.x weapon-attribute preset families this mod's weapons are modelled
 * on.
 *
 * <p>These are plain resource locations on purpose: the mod contains no Better Combat code
 * dependency. Each weapon's definition ({@code data/bettercombat_neonarsenal/weapon_attributes/<id>.json})
 * spells out the concrete weapon attributes and references that preset family's player animations
 * (for example {@code bettercombat:two_handed_slam}). Better Combat resolves those animation ids and
 * preset behaviour when it loads the datapack.</p>
 *
 * <p>Add a constant here when a future weapon is modelled on an existing Better Combat preset.</p>
 */
public final class NeonArsenalPresets {

    public static final String NAMESPACE = "bettercombat";

    /** One-handed sword preset: horizontal slashes + stab, category {@code sword}. */
    public static final ResourceLocation SWORD = of("sword");

    /** Two-handed claymore preset: wide slashes, thrust and overhead slam, category {@code claymore}. */
    public static final ResourceLocation CLAYMORE = of("claymore");

    /** Two-handed double axe preset: wide slash + 360° spin, category {@code double_axe}. */
    public static final ResourceLocation DOUBLE_AXE = of("double_axe");

    /** Two-handed twin blade preset: alternating switch-blade slashes, category {@code twin_blade}. */
    public static final ResourceLocation TWIN_BLADE = of("twin_blade");

    /** Two-handed katana preset: alternating vertical/horizontal slashes, category {@code katana}. */
    public static final ResourceLocation KATANA = of("katana");

    /** One-handed rapier preset: quick horizontal slash + forward stab, category {@code rapier}. */
    public static final ResourceLocation RAPIER = of("rapier");

    /** Two-handed spear preset: long forward stab, +1 reach, category {@code spear}. */
    public static final ResourceLocation SPEAR = of("spear");

    private NeonArsenalPresets() {
    }

    /**
     * Resolves a Better Combat preset path such as {@code "katana"} into
     * {@code bettercombat:katana}. Keeps call sites free of string concatenation.
     */
    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
