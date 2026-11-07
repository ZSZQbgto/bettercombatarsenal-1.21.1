package com.zszq.bettercombatarsenal.client.fx;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.bettercombatarsenal.BetterCombatArsenal;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Glowing sparks along the blade while a <b>Better Combat</b> weapon swing is in progress.
 *
 * <p>Better Combat's own slash trail is made to glow through the per-weapon
 * {@code trail_appearance} data; this class only adds the extra vanilla-style sparks. It detects
 * swings through Better Combat's client API (reflection, so Better Combat stays optional) instead of
 * the attack key, which means vanilla swings never emit particles and the sparks keep going for the
 * whole attack animation, including its latter half.</p>
 *
 * <p>Blade positions are captured per hand (main, off, and non-hand display contexts) from the glow
 * overlay model while it is rendered, so the sparks follow the real blade in first and third person
 * and a two-handed weapon in the main hand is not driven by the off-hand render.</p>
 */
public final class WeaponTrailFx {

    /** Maximum age of a captured blade position; older captures are ignored. */
    private static final long CAPTURE_MAX_AGE_MS = 500L;

    private static final Map<BakedModel, Vec3[]> BLADE_POINTS = new IdentityHashMap<>();
    private static final Set<BakedModel> BLADE_POINTS_SEEN =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean betterCombatChecked;
    private static Class<?> betterCombatClientInterface;
    private static Method isSwingInProgressMethod;
    private static Method getCurrentAttackHandMethod;
    private static Method attackHandItemStackMethod;
    private static Method attackHandIsOffHandMethod;

    private static Capture mainHand;
    private static Capture offHand;
    private static Capture display;

    private static int motionTickCounter;
    private static int idleTickCounter;

    private WeaponTrailFx() {
    }

    /** World-space blade end points captured from one render pass. */
    private static final class Capture {
        private final Vec3 a;
        private final Vec3 b;
        private final long capturedAt;

        private Capture(Vec3 a, Vec3 b, long capturedAt) {
            this.a = a;
            this.b = b;
            this.capturedAt = capturedAt;
        }

        private boolean fresh() {
            return System.currentTimeMillis() - this.capturedAt <= CAPTURE_MAX_AGE_MS;
        }
    }

    private static void initBetterCombat() {
        if (betterCombatChecked) {
            return;
        }
        betterCombatChecked = true;
        try {
            ClassLoader loader = WeaponTrailFx.class.getClassLoader();
            betterCombatClientInterface = Class.forName(
                    "net.bettercombat.api.MinecraftClient_BetterCombat", false, loader);
            isSwingInProgressMethod = betterCombatClientInterface.getMethod("isWeaponSwingInProgress");
            getCurrentAttackHandMethod = betterCombatClientInterface.getMethod("getCurrentAttackHand");
            Class<?> attackHand = Class.forName("net.bettercombat.api.AttackHand", false, loader);
            attackHandItemStackMethod = attackHand.getMethod("itemStack");
            try {
                attackHandIsOffHandMethod = attackHand.getMethod("isOffHand");
            } catch (Throwable ignored) {
                attackHandIsOffHandMethod = null;
            }
            BetterCombatArsenal.LOGGER.info(
                    "Better Combat swing detection ready; blade sparks follow Better Combat animations only.");
        } catch (Throwable ignored) {
            betterCombatClientInterface = null;
            BetterCombatArsenal.LOGGER.info(
                    "Better Combat API not available; no blade sparks are emitted (vanilla swings never emit them).");
        }
    }

    /**
     * Records the two extreme blade points in world space for the frame currently being rendered.
     * Called from the item renderer while the local player's arsenal weapon is drawn. Captures are
     * stored per hand so a main-hand two-handed weapon and an off-hand weapon never overwrite each
     * other.
     */
    public static void captureBlade(BakedModel overlayModel, ItemDisplayContext context,
                                    PoseStack poseStack, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        boolean handContext = context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (handContext && !isLocalPlayerWeapon(player, stack)) {
            // Another entity's held weapon: do not drive the local player's blade sparks with it.
            return;
        }
        Vec3[] points = bladePoints(overlayModel);
        if (points == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        boolean firstPerson = context.firstPerson();
        Capture capture = new Capture(
                toWorld(matrix, points[0], firstPerson, camera),
                toWorld(matrix, points[1], firstPerson, camera),
                System.currentTimeMillis());
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            mainHand = capture;
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            offHand = capture;
        } else {
            // Ground, item frame, shelf/display contexts.
            display = capture;
        }
    }

    /**
     * Emits sparks while a Better Combat swing runs, lingering sparks while moving, and a very light
     * ambient trickle otherwise (also for weapons lying on the ground or in display frames).
     */
    public static void tick(Minecraft minecraft) {
        WeaponGlowConfig config = WeaponGlowConfig.get();
        if (!config.enabled) {
            return;
        }
        if (emitAttackSparks(minecraft, config)) {
            return;
        }
        if (config.motion.particles && config.motion.particlesPerTick > 0) {
            emitMotionSparks(minecraft, config);
        }
        if (config.idle.particles && config.idle.particlesPerEmit > 0) {
            emitIdleSparks(minecraft, config);
        }
    }

    private static boolean emitAttackSparks(Minecraft minecraft, WeaponGlowConfig config) {
        if (!config.trail.particles || config.trail.particlesPerTick <= 0) {
            return false;
        }
        initBetterCombat();
        if (betterCombatClientInterface == null || !betterCombatClientInterface.isInstance(minecraft)) {
            return false;
        }
        ItemStack attackingStack;
        boolean offHandAttack = false;
        try {
            if (!Boolean.TRUE.equals(isSwingInProgressMethod.invoke(minecraft))) {
                return false;
            }
            Object attackHand = getCurrentAttackHandMethod.invoke(minecraft);
            if (attackHand == null) {
                return false;
            }
            attackingStack = (ItemStack) attackHandItemStackMethod.invoke(attackHand);
            if (attackHandIsOffHandMethod != null) {
                offHandAttack = Boolean.TRUE.equals(attackHandIsOffHandMethod.invoke(attackHand));
            }
        } catch (Throwable ignored) {
            return false;
        }
        if (!isArsenalWeapon(attackingStack)) {
            return false;
        }
        Level level = minecraft.level;
        if (level == null) {
            return false;
        }
        // Prefer the blade of the attacking hand; fall back to the other hand if it was not captured.
        Capture capture = fresh(offHandAttack ? offHand : mainHand);
        if (capture == null) {
            capture = fresh(mainHand);
        }
        if (capture == null) {
            capture = fresh(offHand);
        }
        if (capture == null) {
            return false;
        }
        WeaponGlowConfig.Trail trail = config.trail;
        emitAlong(capture, level, trail.particlesPerTick, trail.spread, trail.speed,
                (float) trail.particleScale, trail.particleLifetimeTicks,
                resolveParticleVariants(trail.variants, trail.particle));
        return true;
    }

    /**
     * While the player moves, a few long-lived sparks are left where the blade sweeps. They have
     * almost no velocity, so they stay in place and fade out over the configured lifetime (default
     * 2.5 s).
     */
    private static void emitMotionSparks(Minecraft minecraft, WeaponGlowConfig config) {
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        List<Capture> held = heldCaptures(player);
        if (held.isEmpty()) {
            return;
        }
        double horizontalSpeed = player.getDeltaMovement().horizontalDistance();
        if (horizontalSpeed < config.motion.minSpeed) {
            return;
        }
        if (++motionTickCounter % config.motion.intervalTicks != 0) {
            return;
        }
        WeaponGlowConfig.Motion motion = config.motion;
        List<ParticleOptions> types = resolveParticleVariants(motion.variants, config.trail.particle);
        for (Capture capture : held) {
            emitAlong(capture, level, motion.particlesPerTick, motion.spread, motion.speed,
                    (float) motion.particleScale, motion.particleLifetimeTicks, types);
        }
    }

    /**
     * Very light ambient trickle at the blade, also while standing still. It runs for the held
     * weapon, and otherwise for whichever arsenal weapon was rendered most recently, which covers
     * weapons lying on the ground, in item frames or on shelf/display contexts.
     */
    private static void emitIdleSparks(Minecraft minecraft, WeaponGlowConfig config) {
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        List<Capture> held = player == null ? List.of() : heldCaptures(player);
        if (held.isEmpty()) {
            Capture shown = fresh(display);
            if (shown == null) {
                return;
            }
            held = List.of(shown);
        }
        if (++idleTickCounter % config.idle.intervalTicks != 0) {
            return;
        }
        WeaponGlowConfig.Idle idle = config.idle;
        List<ParticleOptions> types = resolveParticleVariants(idle.variants, config.trail.particle);
        for (Capture capture : held) {
            emitAlong(capture, level, idle.particlesPerEmit, idle.spread, idle.speed,
                    (float) idle.particleScale, idle.particleLifetimeTicks, types);
        }
    }

    private static void emitAlong(Capture capture, Level level, int count, double spread, double speed,
                                  float scale, int lifetime, List<ParticleOptions> types) {
        GlowSparkParticle.nextScale = scale;
        GlowSparkParticle.nextLifetime = lifetime;
        RandomSource random = RandomSource.create();
        for (int i = 0; i < count; i++) {
            double t = random.nextDouble();
            Vec3 point = capture.a.lerp(capture.b, t);
            double x = point.x + (random.nextDouble() - 0.5D) * spread * 2.0D;
            double y = point.y + (random.nextDouble() - 0.5D) * spread * 2.0D;
            double z = point.z + (random.nextDouble() - 0.5D) * spread * 2.0D;
            double vx = (random.nextDouble() - 0.5D) * speed * 2.0D;
            double vy = (random.nextDouble() - 0.5D) * speed * 2.0D;
            double vz = (random.nextDouble() - 0.5D) * speed * 2.0D;
            level.addParticle(pick(types, random), x, y, z, vx, vy, vz);
        }
    }

    private static Capture fresh(Capture capture) {
        return capture != null && capture.fresh() ? capture : null;
    }

    /** Blade of the weapon actually held in a hand (main hand first). */
    private static Capture heldCapture(LocalPlayer player) {
        List<Capture> captures = heldCaptures(player);
        return captures.isEmpty() ? null : captures.get(0);
    }

    /** Fresh blade captures for every hand that holds an arsenal weapon. */
    private static List<Capture> heldCaptures(LocalPlayer player) {
        List<Capture> captures = new ArrayList<>(2);
        if (isArsenalWeapon(player.getMainHandItem())) {
            Capture capture = fresh(mainHand);
            if (capture != null) {
                captures.add(capture);
            }
        }
        if (isArsenalWeapon(player.getOffhandItem())) {
            Capture capture = fresh(offHand);
            if (capture != null) {
                captures.add(capture);
            }
        }
        return captures;
    }

    private static ParticleOptions pick(List<ParticleOptions> types, RandomSource random) {
        return types.get(random.nextInt(types.size()));
    }

    /**
     * Resolves the configured variant list into particle options; empty/invalid entries are skipped
     * and the fallback id is used when nothing resolves.
     */
    private static List<ParticleOptions> resolveParticleVariants(List<String> variants, String fallback) {
        List<ParticleOptions> resolved = new ArrayList<>();
        if (variants != null) {
            for (String variant : variants) {
                ParticleOptions options = tryResolveParticle(variant);
                if (options != null) {
                    resolved.add(options);
                }
            }
        }
        if (resolved.isEmpty()) {
            resolved.add(resolveParticle(fallback));
        }
        return resolved;
    }

    private static ParticleOptions tryResolveParticle(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(location);
        return type instanceof ParticleOptions options ? options : null;
    }

    private static ParticleOptions resolveParticle(String id) {
        ParticleOptions options = tryResolveParticle(id);
        return options != null ? options : ParticleTypes.END_ROD;
    }

    private static Vec3 toWorld(Matrix4f matrix, Vec3 point, boolean firstPerson, Camera camera) {
        Vector3f transformed = new Vector3f((float) point.x, (float) point.y, (float) point.z);
        matrix.transformPosition(transformed);
        if (firstPerson) {
            transformed.rotate(camera.rotation());
        }
        Vec3 cameraPosition = camera.getPosition();
        return new Vec3(transformed.x + cameraPosition.x,
                transformed.y + cameraPosition.y,
                transformed.z + cameraPosition.z);
    }

    /** True when this exact stack is in one of the local player's hands (not another entity's). */
    private static boolean isLocalPlayerWeapon(LocalPlayer player, ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, player.getMainHandItem())
                || ItemStack.isSameItemSameComponents(stack, player.getOffhandItem());
    }

    private static boolean isArsenalWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()
                .equals(BetterCombatArsenal.MODID);
    }

    private static Vec3[] bladePoints(BakedModel model) {
        if (BLADE_POINTS_SEEN.contains(model)) {
            return BLADE_POINTS.get(model);
        }
        Vec3[] points = findBladePoints(model);
        BLADE_POINTS_SEEN.add(model);
        if (points != null) {
            BLADE_POINTS.put(model, points);
        }
        return points;
    }

    /**
     * Finds the two blade points farthest apart in model space, considering only quads whose mask
     * texture contains opaque blade pixels.
     */
    private static Vec3[] findBladePoints(BakedModel model) {
        List<Vec3> points = new ArrayList<>();
        List<Vec3> fallback = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            collectQuadPoints(model.getQuads(null, direction, random), points, fallback);
        }
        collectQuadPoints(model.getQuads(null, null, random), points, fallback);
        List<Vec3> source = points.size() >= 2 ? points : fallback;
        if (source.size() < 2) {
            return null;
        }
        Vec3 bestA = null;
        Vec3 bestB = null;
        double bestDistance = -1.0D;
        for (int i = 0; i < source.size(); i++) {
            for (int j = i + 1; j < source.size(); j++) {
                double distance = source.get(i).distanceToSqr(source.get(j));
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestA = source.get(i);
                    bestB = source.get(j);
                }
            }
        }
        return bestA == null || bestB == null ? null : new Vec3[]{bestA, bestB};
    }

    private static void collectQuadPoints(List<BakedQuad> quads, List<Vec3> points, List<Vec3> fallback) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            TextureAtlasSprite sprite = quad.getSprite();
            SpriteContents contents = sprite.contents();
            if (contents == null) {
                continue;
            }
            float u0 = sprite.getU0();
            float v0 = sprite.getV0();
            float uRange = sprite.getU1() - u0;
            float vRange = sprite.getV1() - v0;
            if (uRange <= 0.0F || vRange <= 0.0F) {
                continue;
            }
            boolean hasBladePixel = false;
            List<Vec3> quadPoints = new ArrayList<>(4);
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                Vec3 position = new Vec3(
                        Float.intBitsToFloat(data[base]),
                        Float.intBitsToFloat(data[base + 1]),
                        Float.intBitsToFloat(data[base + 2]));
                quadPoints.add(position);
                fallback.add(position);
                float u = Float.intBitsToFloat(data[base + 4]);
                float v = Float.intBitsToFloat(data[base + 5]);
                int px = Mth.clamp((int) ((u - u0) / uRange * contents.width()), 0, contents.width() - 1);
                int py = Mth.clamp((int) ((v - v0) / vRange * contents.height()), 0, contents.height() - 1);
                if (!contents.isTransparent(0, px, py)) {
                    hasBladePixel = true;
                }
            }
            if (hasBladePixel) {
                points.addAll(quadPoints);
            }
        }
    }
}
