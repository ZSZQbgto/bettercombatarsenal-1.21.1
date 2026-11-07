package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Anvil forging ("锻刃"): weapon + blaze rod or chorus fruit, consuming experience levels.
 * A weapon supports up to {@link Forge#MAX_LAYERS} layers; each layer adds a colour and an effect.
 */
public final class ForgeAnvil {

    private static final int CHORUS_COST = 8;

    private ForgeAnvil() {
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!(left.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        ItemStack right = event.getRight();
        if (right.is(Items.GLOW_INK_SAC)) {
            if (Forge.glowEnabled(left)) {
                // Already forged: the anvil no longer accepts another glow ink sac.
                return;
            }
            // Basic switch: enables the blade glow and the BC slash-trail colour override.
            ItemStack output = left.copy();
            output.set(com.zszq.neonarsenal.registry.ModDataComponents.GLOW_FORGE.get(), 1);
            event.setOutput(output);
            event.setCost(1);
            event.setMaterialCost(1);
            return;
        }
        if (right.is(Items.BLAZE_POWDER)) {
            if (Forge.particlesEnabled(left)) {
                // Already forged: the anvil no longer accepts another blaze powder.
                return;
            }
            // Basic switch: enables the spark particles.
            ItemStack output = left.copy();
            output.set(com.zszq.neonarsenal.registry.ModDataComponents.PARTICLE_FORGE.get(), 1);
            event.setOutput(output);
            event.setCost(1);
            event.setMaterialCost(1);
            return;
        }
        if (right.getItem() instanceof net.minecraft.world.item.DyeItem dyeItem) {
            // Dyes can be forged unlimited times; each dye blends 50/50 with the current colour.
            // Only the resulting single colour is stored (no history, no nested data).
            int next = dyeItem.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
            int current = Forge.dyeColor(left);
            int blended = current < 0 ? next : Forge.blendDye(current, next);
            ItemStack output = left.copy();
            output.set(com.zszq.neonarsenal.registry.ModDataComponents.DYE.get(), blended);
            event.setOutput(output);
            event.setCost(1);
            event.setMaterialCost(1);
            return;
        }
        String layer;
        int cost;
        if (right.is(Items.CHORUS_FRUIT)) {
            layer = Forge.CHORUS;
            cost = CHORUS_COST;
        } else if (right.is(Items.CREEPER_HEAD)) {
            layer = Forge.CREEPER;
            cost = 8;
        } else if (right.is(Items.RABBIT_FOOT)) {
            layer = Forge.RABBIT;
            cost = 8;
        } else if (right.is(Items.ECHO_SHARD)) {
            layer = Forge.ECHO;
            cost = 8;
        } else {
            return;
        }
        if (!Forge.canAdd(left, layer)) {
            return;
        }
        ItemStack output = left.copy();
        Forge.add(output, layer);
        // No real enchantment is applied: the fire effect is handled on hit, so the weapon never
        // shows the enchantment glint.
        event.setOutput(output);
        event.setCost(cost);
        event.setMaterialCost(1);
    }
}
