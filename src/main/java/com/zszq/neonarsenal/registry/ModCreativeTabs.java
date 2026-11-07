package com.zszq.neonarsenal.registry;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative mode integration: a dedicated arsenal tab plus entries in the vanilla Combat tab.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NeonArsenal.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL = TABS.register("arsenal", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bettercombat_neonarsenal"))
                    // No withTabsBefore()/withTabsAfter() constraints: NeoForge places unknown modded
                    // tabs after the vanilla tabs on its own, and mixing tab ordering constraints has
                    // been observed to produce a CreativeModeTab ordering cycle on NeoForge 21.1.265.
                    .icon(() -> new ItemStack(ModItems.get("claymore").get()))
                    .displayItems((parameters, output) -> ModItems.all()
                            .forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
