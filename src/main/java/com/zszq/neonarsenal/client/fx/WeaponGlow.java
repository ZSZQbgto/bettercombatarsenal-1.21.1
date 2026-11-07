package com.zszq.neonarsenal.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side blade glow.
 *
 * <p>The supplied {@code <weapon>_blade.png} files are mask textures: they are opaque exactly on the
 * blade pixels of the weapon texture and transparent everywhere else. Each weapon gets an overlay
 * model ({@code item/<id>_fx}) that reuses the original geometry with texture key {@code 1} bound to
 * that mask, so drawing the overlay only touches the authored blade area.</p>
 *
 * <p>The overlay is drawn with full-bright light (so it ignores the world light level) and, by
 * default, additive blending so it reads as a self-lit glow instead of a flat painted layer. No
 * supplied model or texture is edited.</p>
 */
public final class WeaponGlow {

    /** Full-bright, additive overlay layer for item-atlas quads. */
    private static final RenderType ADDITIVE_LAYER = RenderType.create(
            "bettercombat_neonarsenal_blade_glow",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    // Push the overlay slightly toward the camera: without it, quads that are
                    // exactly coplanar with the base model z-fight and only part of the blade
                    // appears lit (the "incomplete glow" case).
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /** Self-lit overlay layer handled by Iris shader packs; binds the item atlas explicitly. */
    private static final RenderType EMISSIVE_LAYER =
            RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS);

    /**
     * First-person variant of the Iris-compatible emissive layer. It is the same self-lit render
     * type (NEW_ENTITY format, entity-translucent-emissive shader) but with polygon offset applied.
     * Without the offset the overlay is exactly coplanar with the hand model; while the weapon is
     * static the depth comparison then hides the emissive pass behind the hand model (the blade
     * looks grey from the base texture), and only during animation does it flicker through. The
     * offset pushes the overlay in front so the blade is uniformly lit while idle as well.
     */
    private static final RenderType EMISSIVE_LAYER_FIRST_PERSON = RenderType.create(
            "bettercombat_neonarsenal_blade_glow_emissive_hand",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * Additive, full-bright layer used by the deferred first-person pass. It keeps depth testing and
     * back-face culling but has no polygon offset, so the glow is only visible where the blade
     * surface itself is visible and never shines through the weapon model.
     */
    private static final RenderType DEFERRED_LAYER = RenderType.create(
            "bettercombat_neonarsenal_blade_glow_deferred",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private static final RandomSource RANDOM = RandomSource.create(42L);

    /** Cached "Iris shader pack in use" probe; re-checked at most every two seconds. */
    private static Boolean shadersActive;
    private static long shadersCheckedAt;

    private WeaponGlow() {
    }

    /**
     * Draws the blade glow for one item, using the same pose the caller already used for the base
     * model. Must be called right after the base 3D model was rendered so the overlay is flushed on
     * top and depth-compares equal.
     */
    public static void render(ItemStack stack, ResourceLocation itemId, ItemDisplayContext context,
                              PoseStack poseStack, MultiBufferSource buffers) {
        if (stack.isEmpty()) {
            return;
        }
        WeaponGlowConfig config = WeaponGlowConfig.get();
        if (!config.enabled) {
            return;
        }

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        ResourceLocation overlayId = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath() + "_fx");
        BakedModel overlay = modelManager.getModel(ModelResourceLocation.standalone(overlayId));
        if (overlay == modelManager.getMissingModel()) {
            return;
        }

        float sharpnessFactor = com.zszq.neonarsenal.item.Sharpness.glowFactor(stack);
        if (config.glow.enabled && config.glow.alpha > 0.0F && sharpnessFactor > 0.0F
                && com.zszq.neonarsenal.item.Forge.glowEnabled(stack)) {
            float[] tint = config.tintColor();
            int dye = com.zszq.neonarsenal.item.Forge.dyeColor(stack);
            if (dye >= 0) {
                tint = new float[]{((dye >> 16) & 0xFF) / 255.0F,
                        ((dye >> 8) & 0xFF) / 255.0F,
                        (dye & 0xFF) / 255.0F};
            } else if (!com.zszq.neonarsenal.item.Forge.layers(stack).isEmpty()) {
                int forgeColor = com.zszq.neonarsenal.item.Forge.color(stack);
                tint = new float[]{((forgeColor >> 16) & 0xFF) / 255.0F,
                        ((forgeColor >> 8) & 0xFF) / 255.0F,
                        (forgeColor & 0xFF) / 255.0F};
            }
            float alpha = config.glow.alpha * sharpnessFactor
                    * (float) com.zszq.neonarsenal.config.NeonArsenalConfig.bladeGlowIntensity();
            if (context.firstPerson() && shaderPackActive()) {
                // Iris adaption: blending inside the hand pass is controlled by the shader pack, so
                // the glow is drawn after the hands (GUI pre phase) with the captured matrices.
                FirstPersonGlowPass.capture(context, overlay, poseStack, tint, alpha);
            } else {
                PoseStack.Pose pose = poseStack.last();
                drawOverlay(buffers.getBuffer(layerFor(config, context.firstPerson())), pose, overlay,
                        tint, alpha);
            }
        }

        if (config.trail.particles || config.motion.particles || config.idle.particles) {
            // Reuse the same overlay model to locate the blade so all sparks follow it, in hand and
            // in display contexts (ground, item frames, shelves, ...).
            WeaponTrailFx.captureBlade(overlay, context, poseStack, stack);
        }
    }

    private static void drawOverlay(VertexConsumer buffer, PoseStack.Pose pose, BakedModel overlay,
                                    float[] tint, float alpha) {
        for (Direction direction : Direction.values()) {
            writeQuads(buffer, pose, overlay.getQuads(null, direction, RANDOM), tint, alpha);
        }
        writeQuads(buffer, pose, overlay.getQuads(null, null, RANDOM), tint, alpha);
    }

    /** Additive, full-bright layer used by the deferred first-person pass. */
    public static RenderType deferredLayer() {
        return DEFERRED_LAYER;
    }

    /** Draws every overlay quad; shared with the deferred first-person pass. */
    public static void drawOverlayQuads(VertexConsumer buffer, PoseStack.Pose pose, BakedModel overlay,
                                        float[] tint, float alpha) {
        drawOverlay(buffer, pose, overlay, tint, alpha);
    }

    /**
     * Selects the render type for the glow overlay.
     *
     * <p>The Iris adaptation itself lives here: shader packs replace the pipeline and blending of
     * custom render types, and their deferred passes shade translucent geometry again, which makes an
     * additive overlay come out partially lit. The vanilla {@code entity_translucent_emissive} layer
     * is a render type shader packs already handle (it is explicitly self-lit), so it is used
     * whenever a shader pack is active. In first person the same emissive layer is used, only with
     * polygon offset, so it is not hidden behind the hand model while the weapon is static.</p>
     */
    private static RenderType layerFor(WeaponGlowConfig config, boolean firstPerson) {
        String mode = config.glow.mode;
        if ("emissive".equals(mode)) {
            return firstPerson ? EMISSIVE_LAYER_FIRST_PERSON : EMISSIVE_LAYER;
        }
        if ("translucent".equals(mode)) {
            return Sheets.translucentItemSheet();
        }
        if ("additive".equals(mode)) {
            return ADDITIVE_LAYER;
        }
        if (shaderPackActive()) {
            return firstPerson ? EMISSIVE_LAYER_FIRST_PERSON : EMISSIVE_LAYER;
        }
        return config.glow.additive ? ADDITIVE_LAYER : Sheets.translucentItemSheet();
    }

    /**
     * Optional Iris integration without a compile-time dependency: the Iris API is looked up
     * reflectively so the mod keeps working (and stays loadable) when Iris is absent.
     */
    private static boolean shaderPackActive() {
        long now = System.currentTimeMillis();
        if (shadersActive == null || now - shadersCheckedAt > 2000L) {
            shadersCheckedAt = now;
            shadersActive = detectShaderPack();
        }
        return shadersActive;
    }

    private static boolean detectShaderPack() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object result = api.getMethod("isShaderPackInUse").invoke(instance);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void writeQuads(VertexConsumer buffer, PoseStack.Pose pose, java.util.List<BakedQuad> quads,
                                   float[] tint, float alpha) {
        for (BakedQuad quad : quads) {
            // Full-bright lightmap + no overlay: the glow is independent of world light and damage.
            buffer.putBulkData(pose, quad, tint[0], tint[1], tint[2], alpha,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
