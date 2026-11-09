package com.zszq.neonarsenal.client;

import java.util.List;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.client.fx.FirstPersonGlowPass;
import com.zszq.neonarsenal.client.fx.GlowSparkParticle;
import com.zszq.neonarsenal.client.fx.WeaponGlowConfig;
import com.zszq.neonarsenal.client.fx.WeaponTrailFx;
import com.zszq.neonarsenal.registry.ModItems;
import com.zszq.neonarsenal.registry.ModParticles;
import com.zszq.neonarsenal.weapon.WeaponCatalog;
import com.zszq.neonarsenal.weapon.WeaponDefinition;

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
 * <p>Every weapon renders through {@link NeonArsenalItemRenderer}: the supplied 16x16 PNGs from
 * {@code resourcepacks/item} are used <b>only</b> for the GUI/inventory thumbnail
 * ({@code item/<id>_thumbnail.json}), while every in-world context (first/third person, ground,
 * item frame, ...) renders the full 3D model ({@code item/<id>_3d.json}).</p>
 *
 * <p>Both model variants are registered as additional standalone models here so the custom
 * renderer can look them up by convention: {@code item/<id>_3d} and {@code item/<id>_thumbnail}.
 * Adding a new weapon therefore stays data-driven — no code changes for new weapon ids.</p>
 */
@Mod(value = NeonArsenal.MODID, dist = Dist.CLIENT)
public final class NeonArsenalClient {

    private int configCheckTicks;

    public NeonArsenalClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::registerGeometryLoaders);
        modEventBus.addListener(this::registerParticleProviders);
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(com.zszq.neonarsenal.client.FallSlamClient::preTick);
        NeoForge.EVENT_BUS.addListener(com.zszq.neonarsenal.client.ChargeThrustClient::preTick);
        NeoForge.EVENT_BUS.addListener(com.zszq.neonarsenal.client.ChargeThrustClient::tick);
        NeoForge.EVENT_BUS.addListener(FirstPersonGlowPass::render);
        NeoForge.EVENT_BUS.addListener(NeonArsenalClient::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(com.zszq.neonarsenal.client.fx.BlockShakeEffects::onRenderHand);
        NeoForge.EVENT_BUS.addListener(com.zszq.neonarsenal.client.fx.BlockShakeEffects::onCameraAngles);
    }

    /**
     * The arsenal weapons draw their own enchantment section (numeric bonuses by default, names and
     * effects while Shift is held), so the plain vanilla enchantment-name lines are removed from
     * their tooltip to avoid showing the same information twice and to keep the default tooltip to
     * the numeric bonuses only.
     */
    private static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof com.zszq.neonarsenal.item.SharpnessWeaponItem)) {
            return;
        }
        java.util.List<net.minecraft.network.chat.Component> tooltip = event.getToolTip();
        for (var entry : net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentsForCrafting(stack).entrySet()) {
            tooltip.remove(net.minecraft.world.item.enchantment.Enchantment
                    .getFullname(entry.getKey(), entry.getValue()));
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(WeaponGlowConfig::load);
    }

    private void clientTick(ClientTickEvent.Post event) {
        WeaponTrailFx.tick(Minecraft.getInstance());
        com.zszq.neonarsenal.client.fx.TrailFollow.tick();
        com.zszq.neonarsenal.client.fx.BlockShakeEffects.tick();
        com.zszq.neonarsenal.client.FallSlamClient.tick();
        if (Minecraft.getInstance().player != null
                && com.zszq.neonarsenal.client.BlockPose.isBlocking(Minecraft.getInstance().player)) {
            // Blocked hits should not flash the player red.
            Minecraft.getInstance().player.hurtTime = 0;
            Minecraft.getInstance().player.hurtDuration = 0;
            if (Minecraft.getInstance().player.isShiftKeyDown()) {
                Minecraft.getInstance().player.setShiftKeyDown(false);
            }
            if (Minecraft.getInstance().player.isSprinting()) {
                Minecraft.getInstance().player.setSprinting(false);
            }
        }
        if (++configCheckTicks >= 20) {
            configCheckTicks = 0;
            WeaponGlowConfig.reloadIfChanged();
        }
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW_SPARK.get(),
                sprites -> new GlowSparkParticle.Provider(sprites, 1.0F, 1.0F, 1.0F, 1.0F));
        event.registerSpriteSet(ModParticles.BLOCK_SPARK.get(),
                sprites -> new com.zszq.neonarsenal.client.fx.BlockSparkParticle.Provider(sprites));
        event.registerSpriteSet(ModParticles.GLOW_GLITTER.get(),
                sprites -> new GlowSparkParticle.Provider(sprites, 1.0F, 1.0F, 1.0F, 0.85F));
        event.registerSpriteSet(ModParticles.GLOW_SPARKLE.get(),
                sprites -> new GlowSparkParticle.Provider(sprites, 1.0F, 1.0F, 1.0F, 0.9F));
        event.registerSpriteSet(ModParticles.GLOW_CRIT.get(),
                sprites -> new GlowSparkParticle.Provider(sprites, 1.0F, 1.0F, 1.0F, 1.05F));
        event.registerSpriteSet(ModParticles.GLOW_MAGIC.get(),
                sprites -> new GlowSparkParticle.Provider(sprites, 1.0F, 1.0F, 1.0F, 0.95F));
    }

    private void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(PermissiveModelLoader.ID, new PermissiveModelLoader());
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (WeaponDefinition weapon : WeaponCatalog.all()) {
            event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_3d")));
            event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_fx")));
            if (hasModel(weapon.id() + "_thumbnail")) {
                event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_thumbnail")));
            }
            if (hasModel(weapon.id() + "_fpparry")) {
                event.register(ModelResourceLocation.standalone(model("item/" + weapon.id() + "_fpparry")));
            }
        }
    }

    /**
     * Weapons without a supplied 16x16 icon have no {@code <id>_thumbnail} model; the renderer then
     * falls back to the 3D model in the GUI instead of trying to load a missing model.
     */
    private static boolean hasModel(String path) {
        return NeonArsenalClient.class.getResource(
                "/assets/" + NeonArsenal.MODID + "/models/item/" + path + ".json") != null;
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        List<Item> weapons = WeaponCatalog.all().stream()
                .map(weapon -> ModItems.get(weapon.id()).get())
                .toList();
        event.registerItem(NeonArsenalItemRenderer.EXTENSION, weapons.toArray(Item[]::new));
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, path);
    }

}
