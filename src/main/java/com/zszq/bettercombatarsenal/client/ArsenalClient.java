package com.zszq.bettercombatarsenal.client;

import java.util.List;

import com.zszq.bettercombatarsenal.BetterCombatArsenal;
import com.zszq.bettercombatarsenal.client.fx.GlowSparkParticle;
import com.zszq.bettercombatarsenal.client.fx.WeaponGlowConfig;
import com.zszq.bettercombatarsenal.client.fx.WeaponTrailFx;
import com.zszq.bettercombatarsenal.registry.ModItems;
import com.zszq.bettercombatarsenal.registry.ModParticles;
import com.zszq.bettercombatarsenal.weapon.WeaponCatalog;
import com.zszq.bettercombatarsenal.weapon.WeaponDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only wiring for the arsenal weapons.
 *
 * <p>Every weapon renders through {@link ArsenalItemRenderer}: the supplied 16x16 PNGs from
 * {@code resourcepacks/item} are used <b>only</b> for the GUI/inventory thumbnail
 * ({@code item/<id>_thumbnail.json}), while every in-world context (first/third person, ground,
 * item frame, ...) renders the full 3D model ({@code item/<id>_3d.json}).</p>
 *
 * <p>Both model variants are registered as additional standalone models here so the custom
 * renderer can look them up by convention: {@code item/<id>_3d} and {@code item/<id>_thumbnail}.
 * Adding a new weapon therefore stays data-driven — no code changes for new weapon ids.</p>
 */
@Mod(value = BetterCombatArsenal.MODID, dist = Dist.CLIENT)
public final class ArsenalClient {

    private int configCheckTicks;

    public ArsenalClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::registerGeometryLoaders);
        modEventBus.addListener(this::registerParticleProviders);
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::clientTick);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(WeaponGlowConfig::load);
    }

    private void clientTick(ClientTickEvent.Post event) {
        WeaponTrailFx.tick(Minecraft.getInstance());
        if (++configCheckTicks >= 20) {
            configCheckTicks = 0;
            WeaponGlowConfig.reloadIfChanged();
        }
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW_SPARK.get(), GlowSparkParticle.Provider::new);
    }

    private void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(PermissiveModelLoader.ID, new PermissiveModelLoader());
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (WeaponDefinition weapon : WeaponCatalog.all()) {
            event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_3d")));
            event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_fx")));
            event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_thumbnail")));
        }
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        List<Item> weapons = WeaponCatalog.all().stream()
                .map(weapon -> ModItems.get(weapon.id()).get())
                .toList();
        event.registerItem(ArsenalItemRenderer.EXTENSION, weapons.toArray(Item[]::new));
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterCombatArsenal.MODID, path);
    }
}
