package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's {@code AnvilMenu#mayPickup} requires {@code cost > 0}; the honeycomb sharpness repair
 * is intentionally free, so its result would otherwise be impossible to take out of the anvil.
 * Allow pickup only for exactly that case (our weapon in the left slot, honeycomb on the right,
 * cost 0), leaving normal anvil behaviour untouched.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuFreeRepairMixin {

    @Shadow
    @Final
    private DataSlot cost;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void neonarsenal$allowFreeHoneycombRepair(Player player, boolean hasStack,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (!hasStack || this.cost.get() != 0) {
            return;
        }
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();
        if (left.getItem() instanceof SharpnessWeaponItem && right.is(Items.HONEYCOMB)) {
            cir.setReturnValue(true);
        }
    }
}
