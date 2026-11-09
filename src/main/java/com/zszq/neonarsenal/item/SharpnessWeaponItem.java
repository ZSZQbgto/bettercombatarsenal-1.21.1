package com.zszq.neonarsenal.item;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.zszq.neonarsenal.weapon.WeaponDefinition.Ability;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A non-damageable arsenal weapon: no durability, never breaks, and carries a sharpness index plus
 * forge layers.
 *
 * <p>Tooltip: a compact summary is always shown; holding <b>Shift</b> additionally shows the final
 * total stats and a description of every forge effect. The text is fully localised (EN/ZH).</p>
 */
public class SharpnessWeaponItem extends Item {

    private static final String PREFIX = "tooltip.bettercombat_neonarsenal.";
    private static Method hasShiftDown;
    private static boolean shiftLookupTried;

    private final String weaponId;
    private final Tier tier;
    private final Set<Ability> abilities;

    public SharpnessWeaponItem(String weaponId, Tier tier, Item.Properties properties) {
        this(weaponId, tier, properties, Set.of());
    }

    public SharpnessWeaponItem(String weaponId, Tier tier, Item.Properties properties, Set<Ability> abilities) {
        super(properties);
        this.weaponId = weaponId;
        this.tier = tier;
        this.abilities = Set.copyOf(abilities);
    }

    /** {@return the catalogue id of this weapon (for example {@code claymore}).} */
    public String weaponId() {
        return this.weaponId;
    }

    /**
     * Arsenal weapons are not damageable, so the vanilla "has max damage" test would never make
     * them enchantable; allow the enchanting table, the anvil and the {@code /enchant} command.
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * Arsenal weapons use their own blade glow (the flowing light once a glow ink sac is forged) and
     * never the vanilla enchantment glint, so enchanted weapons stay visually identical to the mod's
     * own effect.
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return com.zszq.neonarsenal.weapon.WeaponEnchantments.supports(this.weaponId, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return com.zszq.neonarsenal.weapon.WeaponEnchantments.supports(this.weaponId, enchantment);
    }

    /** {@return whether this weapon may perform the given skill}. */
    public boolean hasAbility(Ability ability) {
        return this.abilities.contains(ability);
    }

    /** {@return true when this weapon may be used for right-click blocking}. */
    public boolean canBlock() {
        return hasAbility(Ability.BLOCK);
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.getEnchantmentValue();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        // Weapons without the blocking ability keep plain item behaviour.
        if (!canBlock()) {
            return super.use(level, player, usedHand);
        }
        if (usedHand == InteractionHand.MAIN_HAND) {
            // While gliding, right-click has no effect (the elytra slash is a left-click hold), so the
            // use does not start blocking.
            if (player.isFallFlying()) {
                return InteractionResultHolder.fail(player.getItemInHand(usedHand));
            }
            // Blocking has priority over item/attack/switch cooldowns: clear any item cooldown.
            player.getCooldowns().removeCooldown(this);
            // Blocking cannot be combined with sprinting: interrupt an active sprint immediately.
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /** Allow placing arsenal weapons in a grindstone (used for the forging wipe). */
    @Override
    public boolean canGrindstoneRepair(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.HONEYCOMB) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int sharpness = Sharpness.get(stack);
        ChatFormatting sharpnessColor = sharpness >= Sharpness.MAX * 3 / 4 ? ChatFormatting.GREEN
                : sharpness >= Sharpness.MAX / 4 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.translatable(PREFIX + "sharpness", sharpness, Sharpness.MAX)
                .withStyle(sharpnessColor));
        int dye = Forge.dyeColor(stack);
        if (dye >= 0) {
            tooltip.add(Component.translatable(PREFIX + "dye", String.format("%06X", dye))
                    .withStyle(style -> style.withColor(dye)));
        } else if (!Forge.layers(stack).isEmpty()) {
            int colour = Forge.displayColor(stack) & 0xFFFFFF;
            tooltip.add(Component.translatable(PREFIX + "forge_colour", String.format("%06X", colour))
                    .withStyle(style -> style.withColor(colour)));
        }
        List<String> layers = Forge.layers(stack);
        List<Component> enchantBonuses = EnchantmentTips.bonuses(stack);
        double sharpnessPercent = EnchantmentTips.sharpnessPercent(stack);
        boolean shift = shiftDown();

        if (!shift) {
            // Default view: only the numeric bonuses, all damage-type enchantments folded into the
            // same percentage lines the forge bonuses use.
            if (!layers.isEmpty()) {
                double[] bonus = heldBonuses(layers);
                addBonusLine(tooltip, "bonus.damage", bonus[0] + sharpnessPercent / 100.0D);
                addBonusLine(tooltip, "bonus.attack_speed", bonus[1]);
                addBonusLine(tooltip, "bonus.move_speed", bonus[2]);
            } else if (sharpnessPercent > 0.0D) {
                addBonusLine(tooltip, "bonus.damage", sharpnessPercent / 100.0D);
            }
            if (!enchantBonuses.isEmpty()) {
                tooltip.add(Component.translatable(PREFIX + "section_enchant_bonus")
                        .withStyle(ChatFormatting.GOLD));
                enchantBonuses.forEach(tooltip::add);
            }
            if (!layers.isEmpty()) {
                tooltip.add(Component.translatable(PREFIX + "forge_compact", forgeNames(layers))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable(PREFIX + "not_forged")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable(PREFIX + "hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        // Shift view: the detailed enchantment list plus the complete forge/stats description.
        if (!enchantBonuses.isEmpty() || sharpnessPercent > 0.0D) {
            tooltip.add(Component.translatable(PREFIX + "section_enchant").withStyle(ChatFormatting.GOLD));
            EnchantmentTips.details(stack).forEach(tooltip::add);
        }
        if (layers.isEmpty()) {
            tooltip.add(Component.translatable(PREFIX + "not_forged").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable(PREFIX + "section_stats").withStyle(ChatFormatting.GOLD));
        double[] bonus = heldBonuses(layers);
        addBonusLine(tooltip, "bonus.damage", bonus[0]);
        addBonusLine(tooltip, "bonus.attack_speed", bonus[1]);
        addBonusLine(tooltip, "bonus.move_speed", bonus[2]);
        if (Forge.hasRabbit(stack)) {
            tooltip.add(Component.translatable(PREFIX + "jump_height").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(PREFIX + "jump_slam").withStyle(ChatFormatting.GRAY));
        }
        long multiplier = Math.round(Sharpness.damageMultiplier(sharpness) * 100.0D);
        tooltip.add(Component.translatable(PREFIX + "sharpness_multiplier", multiplier)
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable(PREFIX + "section_forge").withStyle(ChatFormatting.GOLD));
        for (String layer : layers) {
            tooltip.add(Component.translatable(layerNameKey(layer))
                    .withStyle(style -> style.withColor(Forge.colorOf(layer))));
            for (String key : descriptionKeys(layer)) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** {@return the "layer + layer + ..." component used by the compact tooltip line}. */
    private static Component forgeNames(List<String> layers) {
        Component names = Component.empty();
        for (int i = 0; i < layers.size(); i++) {
            if (i > 0) {
                names = names.copy().append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY));
            }
            String layer = layers.get(i);
            names = names.copy().append(Component.translatable(layerNameKey(layer))
                    .withStyle(style -> style.withColor(Forge.colorOf(layer))));
        }
        return names;
    }

    private static void addBonusLine(List<Component> tooltip, String labelKey, double value) {
        if (Math.abs(value) < 1.0E-4D) {
            return;
        }
        long percent = Math.round(value * 100.0D);
        ChatFormatting color = percent >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        String amount = (percent >= 0 ? "+" : "") + percent + "%";
        tooltip.add(Component.translatable(PREFIX + "bonus.format",
                        Component.translatable(PREFIX + labelKey), amount)
                .withStyle(color));
    }

    private static String layerNameKey(String layer) {
        return switch (layer) {
            case Forge.CHORUS -> PREFIX + "layer.chorus";
            case Forge.CREEPER -> PREFIX + "layer.creeper";
            case Forge.RABBIT -> PREFIX + "layer.rabbit";
            case Forge.ECHO -> PREFIX + "layer.echo";
            default -> PREFIX + "layer.unknown";
        };
    }

    /** Held bonuses of all layers: {damage, attack speed, movement speed}. */
    private static double[] heldBonuses(List<String> layers) {
        double damage = 0.0D;
        double attackSpeed = 0.0D;
        double moveSpeed = 0.0D;
        for (String layer : layers) {
            switch (layer) {
                case Forge.CHORUS -> {
                    damage += 0.10D;
                    moveSpeed += 0.15D;
                }
                case Forge.CREEPER -> {
                    damage += 0.05D;
                    attackSpeed += 0.15D;
                }
                case Forge.RABBIT -> attackSpeed += 0.05D;
                case Forge.ECHO -> {
                    attackSpeed -= 0.50D;
                    damage += 1.00D;
                }
                default -> {
                    // Unknown layer.
                }
            }
        }
        return new double[] {damage, attackSpeed, moveSpeed};
    }

    private static List<String> descriptionKeys(String layer) {
        return switch (layer) {
            case Forge.CHORUS -> List.of(PREFIX + "forge.chorus.held", PREFIX + "forge.chorus.effect");
            case Forge.CREEPER -> List.of(PREFIX + "forge.creeper.held", PREFIX + "forge.creeper.effect");
            case Forge.RABBIT -> List.of(PREFIX + "forge.rabbit.held", PREFIX + "forge.rabbit.effect");
            case Forge.ECHO -> List.of(PREFIX + "forge.echo.held", PREFIX + "forge.echo.effect");
            default -> List.of(PREFIX + "layer.unknown");
        };
    }

    private static boolean shiftDown() {
        if (!shiftLookupTried) {
            shiftLookupTried = true;
            try {
                Class<?> screen = Class.forName("net.minecraft.client.gui.screens.Screen");
                hasShiftDown = screen.getMethod("hasShiftDown");
            } catch (Throwable ignored) {
                hasShiftDown = null;
            }
        }
        if (hasShiftDown == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(hasShiftDown.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
