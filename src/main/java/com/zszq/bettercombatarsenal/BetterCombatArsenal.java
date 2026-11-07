package com.zszq.bettercombatarsenal;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.zszq.bettercombatarsenal.compat.BetterCombatIntegration;
import com.zszq.bettercombatarsenal.registry.ModCreativeTabs;
import com.zszq.bettercombatarsenal.registry.ModItems;
import com.zszq.bettercombatarsenal.registry.ModParticles;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Better Combat Arsenal - a small, data-driven NeoForge weapon mod.
 *
 * <p>Weapons live in {@link com.zszq.bettercombatarsenal.weapon.WeaponCatalog}; registration and
 * creative tabs are generic. Better Combat attack animations are linked purely through datapack
 * files, so the mod also runs without Better Combat installed.</p>
 */
@Mod(BetterCombatArsenal.MODID)
public final class BetterCombatArsenal {

    public static final String MODID = "bettercombatarsenal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BetterCombatArsenal(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabContents);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.bettercombatarsenal.compat.JumpAttackKnockback::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.bettercombatarsenal.compat.JumpAttackHandler::onLivingJump);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Registered {} arsenal weapon(s): {}", ModItems.all().size(), ModItems.ids());
            BetterCombatIntegration.reportStatus();
            com.zszq.bettercombatarsenal.compat.JumpAttackHandler.reportStatus();
        });
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            ModItems.all().forEach(item -> event.accept(item.get()));
        }
    }
}
