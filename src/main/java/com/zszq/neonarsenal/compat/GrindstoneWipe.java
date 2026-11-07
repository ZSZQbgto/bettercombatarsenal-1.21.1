package com.zszq.neonarsenal.compat;

import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;
import com.zszq.neonarsenal.registry.ModDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.GrindstoneEvent;

/**
 * Grindstone wipe: a forged weapon placed alone in a grindstone can remove all forging data in one
 * operation (forge layers, dye colour, glow/particle switches, per-weapon cooldowns and the rarity
 * bonus), just like vanilla removes enchantments. The sharpness index is kept.
 */
public final class GrindstoneWipe {

    private GrindstoneWipe() {
    }

    public static void onPlaceItem(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        if (!(top.getItem() instanceof SharpnessWeaponItem) || !event.getBottomItem().isEmpty()) {
            return;
        }
        boolean forged = !Forge.layers(top).isEmpty()
                || Forge.dyeColor(top) >= 0
                || Forge.glowEnabled(top)
                || Forge.particlesEnabled(top)
                || top.has(ModDataComponents.COOLDOWNS.get());
        if (!forged) {
            return;
        }
        ItemStack output = top.copy();
        output.remove(ModDataComponents.FORGE.get());
        output.remove(ModDataComponents.DYE.get());
        output.remove(ModDataComponents.GLOW_FORGE.get());
        output.remove(ModDataComponents.PARTICLE_FORGE.get());
        output.remove(ModDataComponents.COOLDOWNS.get());
        output.remove(DataComponents.RARITY);
        event.setOutput(output);
    }
}
