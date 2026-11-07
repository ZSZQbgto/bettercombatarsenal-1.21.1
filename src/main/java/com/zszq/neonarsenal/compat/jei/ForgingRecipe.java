package com.zszq.neonarsenal.compat.jei;

import net.minecraft.world.item.ItemStack;

/** One anvil forging operation shown in JEI: weapon + material -> result (with XP cost). */
public record ForgingRecipe(ItemStack weapon, ItemStack material, ItemStack result, int xp) {
}
