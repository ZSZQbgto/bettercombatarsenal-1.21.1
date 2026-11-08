package com.zszq.neonarsenal;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.zszq.neonarsenal.compat.NeonArsenalIntegration;
import com.zszq.neonarsenal.registry.ModCreativeTabs;
import com.zszq.neonarsenal.registry.ModItems;
import com.zszq.neonarsenal.registry.ModParticles;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Better Combat Arsenal - a small, data-driven NeoForge weapon mod.
 *
 * <p>Weapons live in {@link com.zszq.neonarsenal.weapon.WeaponCatalog}; registration and
 * creative tabs are generic. Better Combat attack animations are linked purely through datapack
 * files, so the mod also runs without Better Combat installed.</p>
 */
@Mod(NeonArsenal.MODID)
public final class NeonArsenal {

    public static final String MODID = "bettercombat_neonarsenal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeonArsenal(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                com.zszq.neonarsenal.config.NeonArsenalConfig.SPEC);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            // Loaded reflectively so the common mod class never references client-only classes.
            try {
                Class.forName("com.zszq.neonarsenal.client.config.ClientConfigBootstrap")
                        .getMethod("register", net.neoforged.fml.ModContainer.class)
                        .invoke(null, modContainer);
            } catch (Throwable ignored) {
                // Client config screen unavailable; the config file still works.
            }
        }
        ModItems.ITEMS.register(modEventBus);
        com.zszq.neonarsenal.registry.ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        com.zszq.neonarsenal.registry.ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.SharpnessEvents::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.SharpnessEvents::onBlockBreak);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.SharpnessEvents::onAnvilUpdate);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeAnvil::onAnvilUpdate);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.GrindstoneWipe::onPlaceItem);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onStunDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onKnockback);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onLivingTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onPlayerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.BlockHandler::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.BlockHandler::onPlayerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.BlockHandler::onRightClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.BlockHandler::onAttackEntity);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.BlockHandler::onLeftClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.ForgeEffects::onLivingJump);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.JumpAttackKnockback::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.JumpAttackHandler::onLivingJump);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.zszq.neonarsenal.compat.JumpAttackHandler::onPlayerTick);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Registered {} arsenal weapon(s): {}", ModItems.all().size(), ModItems.ids());
            NeonArsenalIntegration.reportStatus();
            com.zszq.neonarsenal.compat.JumpAttackHandler.reportStatus();
        });
    }

}
