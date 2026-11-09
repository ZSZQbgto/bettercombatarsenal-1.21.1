package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.core.Holder;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the {@code /enchant} command force any enchantment onto an arsenal weapon.
 *
 * <p>Natural acquisition (enchanting table and anvil) is limited by
 * {@link com.zszq.neonarsenal.weapon.WeaponEnchantments}: density, breach and impaling are not part
 * of any arsenal weapon's natural list, exactly like vanilla weapons that are not a mace or a
 * trident. Command-forced applications stay possible, and the vanilla exclusive-set rules are still
 * enforced by the command itself, so density/breach never stack with sharpness, smite, bane of
 * arthropods or impaling.</p>
 */
@Mixin(EnchantCommand.class)
public abstract class EnchantCommandArsenalMixin {

    @Redirect(method = "enchant",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;supportsEnchantment(Lnet/minecraft/core/Holder;)Z"))
    private static boolean neonarsenal$forceArsenalWeapon(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.getItem() instanceof SharpnessWeaponItem) {
            return true;
        }
        return stack.supportsEnchantment(enchantment);
    }
}
