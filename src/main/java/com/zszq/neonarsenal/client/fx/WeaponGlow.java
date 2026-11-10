package com.zszq.neonarsenal.client.fx;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
 *
 * <p>Once the glow ink sac is forged the glow is no longer a single flat colour: the overlay is
 * emitted vertex by vertex with a flowing colour, so the blade shows medium-speed bands of colour
 * that travel along the blade. The hues stay near the weapon's current colour-ring colour (dye or
 * mixed forge colour), with a matching brightness wave so even an uncoloured blade shows the flow.</p>
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

    /** Plain translucent overlay: same vertex format/polygon offset as the glow, but normal blending
     *  (used for the non-glowing blade tint from forge layers/dye). */
    private static final RenderType TINT_LAYER = RenderType.create(
            "bettercombat_neonarsenal_blade_tint",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * Self-lit overlay layer handled by Iris shader packs; binds the item atlas explicitly.
     *
     * <p>It is the vanilla entity-translucent-emissive render type (NEW_ENTITY format, item atlas) but
     * with polygon offset applied. Without the offset the overlay is exactly coplanar with the weapon
     * model, so while the weapon is static the depth comparison hides the emissive pass behind the
     * model: thin one-handed blades disappear completely and larger blades only flicker through while
     * moving. The offset pushes the overlay in front so the blade is uniformly lit while idle too.
     * This is what used to break third-person glow (first person was drawn deferred and worked).</p>
     */
    private static final RenderType EMISSIVE_LAYER = RenderType.create(
            "bettercombat_neonarsenal_blade_glow_emissive",
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

    /** Blade direction (axis index, min, extent) of every overlay model, in model-space units. */
    private static final Map<BakedModel, float[]> BLADE_COORDS = new WeakHashMap<>();

    /** Flow speed in colour cycles per second ("medium" speed). */
    private static final float FLOW_SPEED = 0.45F;
    /** Number of colour bands along the blade. */
    private static final float FLOW_CYCLES = 1.4F;
    /** Maximum hue distance from the weapon's colour-ring colour, in degrees (narrow variation). */
    private static final float FLOW_HUE = 12.0F;
    /** Weight of the second hue harmonic; keeps the bands organic inside the narrow hue range. */
    private static final float FLOW_HUE_SECOND = 0.25F;
    /** Brightness wave amplitude so even an uncoloured (white) blade visibly flows. */
    private static final float FLOW_BRIGHTNESS = 0.22F;

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
                              PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
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

        // Forged layers and dye tint the blade even before a glow ink sac is forged: draw the
        // blade mask as a plain translucent colour overlay instead of the self-lit glow.
        boolean hasColour = com.zszq.neonarsenal.item.Forge.dyeColor(stack) >= 0
                || !com.zszq.neonarsenal.item.Forge.layers(stack).isEmpty();
        if (!com.zszq.neonarsenal.item.Forge.glowEnabled(stack) && hasColour) {
            // Reuse the glow overlay model and its polygon-offset positioning, but blend normally
            // and use the world light so the tint does not glow.
            VertexConsumer tintBuffer = buffers.getBuffer(TINT_LAYER);
            float[] tint = forgeTint(stack, config);
            for (Direction direction : Direction.values()) {
                writeQuadsWithLight(tintBuffer, poseStack.last(),
                        overlay.getQuads(null, direction, RANDOM), tint, 0.75F, packedLight);
            }
            writeQuadsWithLight(tintBuffer, poseStack.last(),
                    overlay.getQuads(null, null, RANDOM), tint, 0.75F, packedLight);
        }

        float sharpnessFactor = com.zszq.neonarsenal.item.Sharpness.glowFactor(stack);
        if (config.glow.enabled && config.glow.alpha > 0.0F && sharpnessFactor > 0.0F
                && com.zszq.neonarsenal.item.Forge.glowEnabled(stack)) {
            float[] tint = forgeTint(stack, config);
            float alpha = config.glow.alpha * sharpnessFactor
                    * (float) com.zszq.neonarsenal.config.NeonArsenalConfig.bladeGlowIntensity();
            RenderType layer = layerFor(config);
            boolean emissive = layer == EMISSIVE_LAYER;
            boolean fullBright = emissive || layer == ADDITIVE_LAYER || layer == DEFERRED_LAYER;
            if (context.firstPerson() && shaderPackActive()) {
                // Iris adaption: blending inside the hand pass is controlled by the shader pack, so
                // the glow is drawn after the hands (GUI pre phase) with the captured matrices.
                FirstPersonGlowPass.capture(context, overlay, poseStack, tint, alpha);
            } else {
                drawFlowing(buffers.getBuffer(layer), poseStack.last(), overlay, tint, alpha,
                        emissive, fullBright ? LightTexture.FULL_BRIGHT : packedLight);
            }
        }

        if (config.trail.particles || config.motion.particles || config.idle.particles) {
            // Reuse the same overlay model to locate the blade so all sparks follow it, in hand and
            // in display contexts (ground, item frames, shelves, ...).
            WeaponTrailFx.captureBlade(overlay, context, poseStack, stack);
        }
    }

    /** Dye first, then the mixed forge-layer colour, then the configured default tint. */
    private static float[] forgeTint(ItemStack stack, WeaponGlowConfig config) {
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
        return tint;
    }

    /** Additive, full-bright layer used by the deferred first-person pass. */
    public static RenderType deferredLayer() {
        return DEFERRED_LAYER;
    }

    /**
     * Draws the flowing glow overlay; shared with the deferred first-person pass.
     *
     * @param emissive   the target layer is the NEW_ENTITY emissive format (needs overlay/normal)
     * @param packedLight lightmap to use for non-full-bright layers
     */
    public static void drawFlowingOverlayQuads(VertexConsumer buffer, PoseStack.Pose pose, BakedModel overlay,
                                               float[] tint, float alpha, boolean emissive, int packedLight) {
        drawFlowing(buffer, pose, overlay, tint, alpha, emissive, packedLight);
    }

    /** Emits the overlay quads with per-vertex flowing colours. */
    private static void drawFlowing(VertexConsumer buffer, PoseStack.Pose pose, BakedModel overlay,
                                    float[] tint, float alpha, boolean emissive, int packedLight) {
        float[] hsv = rgbToHsv(tint);
        float[] coords = bladeCoords(overlay);
        double seconds = System.nanoTime() / 1.0E9D;
        for (Direction direction : Direction.values()) {
            writeFlowQuads(buffer, pose, overlay.getQuads(null, direction, RANDOM), hsv, coords,
                    alpha, emissive, packedLight, seconds);
        }
        writeFlowQuads(buffer, pose, overlay.getQuads(null, null, RANDOM), hsv, coords,
                alpha, emissive, packedLight, seconds);
    }

    private static void writeFlowQuads(VertexConsumer buffer, PoseStack.Pose pose, List<BakedQuad> quads,
                                       float[] hsv, float[] coords, float alpha, boolean emissive,
                                       int packedLight, double seconds) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            Direction direction = quad.getDirection();
            for (int i = 0; i < 4; i++) {
                int offset = i * stride;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);
                float u = Float.intBitsToFloat(data[offset + 4]);
                float v = Float.intBitsToFloat(data[offset + 5]);
                float[] rgb = flowColor(hsv, bladeCoord(coords, x, y, z), seconds);
                buffer.addVertex(pose, x, y, z)
                        .setColor(rgb[0], rgb[1], rgb[2], alpha)
                        .setUv(u, v);
                if (emissive) {
                    buffer.setOverlay(OverlayTexture.NO_OVERLAY);
                    buffer.setLight(LightTexture.FULL_BRIGHT);
                    buffer.setNormal(pose, direction.getStepX(), direction.getStepY(), direction.getStepZ());
                } else {
                    buffer.setLight(packedLight);
                }
            }
        }
    }

    /**
     * {@return the blade direction of the overlay model: axis index, minimum and extent, in model
     * units}. The overlay only covers the authored blade pixels, so the longest extent is the blade
     * direction regardless of the weapon's model orientation.
     */
    private static float[] bladeCoords(BakedModel overlay) {
        float[] cached = BLADE_COORDS.get(overlay);
        if (cached != null) {
            return cached;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            float[] bounds = bounds(overlay.getQuads(null, direction, RANDOM));
            minX = Math.min(minX, bounds[0]);
            minY = Math.min(minY, bounds[1]);
            minZ = Math.min(minZ, bounds[2]);
            maxX = Math.max(maxX, bounds[3]);
            maxY = Math.max(maxY, bounds[4]);
            maxZ = Math.max(maxZ, bounds[5]);
        }
        float[] bounds = bounds(overlay.getQuads(null, null, RANDOM));
        minX = Math.min(minX, bounds[0]);
        minY = Math.min(minY, bounds[1]);
        minZ = Math.min(minZ, bounds[2]);
        maxX = Math.max(maxX, bounds[3]);
        maxY = Math.max(maxY, bounds[4]);
        maxZ = Math.max(maxZ, bounds[5]);
        float extentX = maxX - minX;
        float extentY = maxY - minY;
        float extentZ = maxZ - minZ;
        int axis;
        if (extentY >= extentX && extentY >= extentZ) {
            axis = 1;
        } else if (extentX >= extentZ) {
            axis = 0;
        } else {
            axis = 2;
        }
        float min = axis == 0 ? minX : axis == 1 ? minY : minZ;
        float extent = Math.max(1.0E-4F, axis == 0 ? extentX : axis == 1 ? extentY : extentZ);
        float[] result = new float[]{axis, min, extent};
        BLADE_COORDS.put(overlay, result);
        return result;
    }

    /** {@return {minX, minY, minZ, maxX, maxY, maxZ} of one quad list}. */
    private static float[] bounds(List<BakedQuad> quads) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int i = 0; i < 4; i++) {
                int offset = i * stride;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** {@return the normalised position along the blade, 0..1}. */
    private static float bladeCoord(float[] coords, float x, float y, float z) {
        float value = switch ((int) coords[0]) {
            case 0 -> x;
            case 1 -> y;
            default -> z;
        };
        return Math.max(0.0F, Math.min(1.0F, (value - coords[1]) / coords[2]));
    }

    /**
     * {@return the flowing colour at one blade position}. The hue moves around the weapon's colour on
     * the hue ring inside a narrow range (the second harmonic keeps the bands organic), and the
     * brightness follows the same wave so an uncoloured blade still shows the flow.
     */
    private static float[] flowColor(float[] hsv, float bladeCoord, double seconds) {
        double phase = (bladeCoord * FLOW_CYCLES + seconds * FLOW_SPEED) * Math.PI * 2.0D;
        float wave = (float) Math.sin(phase);
        float hue = hsv[0] + FLOW_HUE
                * (wave + FLOW_HUE_SECOND * (float) Math.sin(phase * 2.0D - 1.1D));
        float value = hsv[2] * (1.0F + FLOW_BRIGHTNESS * wave);
        return hsvToRgb(hue, hsv[1], Math.max(0.0F, Math.min(1.0F, value)));
    }

    private static float[] rgbToHsv(float[] rgb) {
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            hue = 60.0F * (((b - r) / delta) + 2.0F);
        } else {
            hue = 60.0F * (((r - g) / delta) + 4.0F);
        }
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        float saturation = max == 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    private static float[] hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        int sector = ((int) Math.floor(h)) % 6;
        float f = h - (float) Math.floor(h);
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * f);
        float t = value * (1.0F - saturation * (1.0F - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }
        return new float[]{r, g, b};
    }

    /**
     * Selects the render type for the glow overlay.
     *
     * <p>The Iris adaptation itself lives here: shader packs replace the pipeline and blending of
     * custom render types, and their deferred passes shade translucent geometry again, which makes an
     * additive overlay come out partially lit. The vanilla {@code entity_translucent_emissive} layer
     * is a render type shader packs already handle (it is explicitly self-lit), so it is used
     * whenever a shader pack is active. That emissive layer carries polygon offset in both hand and
     * world contexts, so the overlay is never hidden behind the weapon model while it is static.</p>
     */
    private static RenderType layerFor(WeaponGlowConfig config) {
        String mode = config.glow.mode;
        if ("emissive".equals(mode)) {
            return EMISSIVE_LAYER;
        }
        if ("translucent".equals(mode)) {
            return TINT_LAYER;
        }
        if ("additive".equals(mode)) {
            return ADDITIVE_LAYER;
        }
        if (shaderPackActive()) {
            return EMISSIVE_LAYER;
        }
        return config.glow.additive ? ADDITIVE_LAYER : TINT_LAYER;
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

    /** Same as {@link #writeQuads} but samples the given packed light instead of full-bright. */
    private static void writeQuadsWithLight(VertexConsumer buffer, PoseStack.Pose pose, java.util.List<BakedQuad> quads,
                                            float[] tint, float alpha, int packedLight) {
        for (BakedQuad quad : quads) {
            buffer.putBulkData(pose, quad, tint[0], tint[1], tint[2], alpha,
                    packedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}
