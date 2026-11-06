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
 * <p>Blade positions are captured from the glow overlay model while it is rendered, so the sparks
 * follow the real blade in first and third person.</p>
 */
public final class WeaponTrailFx {

    private static final Map<BakedModel, Vec3[]> BLADE_POINTS = new IdentityHashMap<>();
    private static final Set<BakedModel> BLADE_POINTS_SEEN =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean betterCombatChecked;
    private static Class<?> betterCombatClientInterface;
    private static Method isSwingInProgressMethod;
    private static Method getCurrentAttackHandMethod;
    private static Method attackHandItemStackMethod;

    private static Vec3 bladeA;
    private static Vec3 bladeB;
    private static long capturedAt;
    private static int motionTickCounter;

    private WeaponTrailFx() {
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
            attackHandItemStackMethod = Class.forName("net.bettercombat.api.AttackHand", false, loader)
                    .getMethod("itemStack");
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
     * Called from the item renderer while the local player's arsenal weapon is drawn.
     */
    public static void captureBlade(BakedModel overlayModel, ItemDisplayContext context,
                                    PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!holdingArsenalWeapon(player)) {
            return;
        }
        if (!context.firstPerson() && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return;
        }
        Vec3[] points = bladePoints(overlayModel);
        if (points == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        bladeA = toWorld(matrix, points[0], context.firstPerson(), camera);
        bladeB = toWorld(matrix, points[1], context.firstPerson(), camera);
        capturedAt = System.currentTimeMillis();
    }

    /** Emits sparks while a Better Combat swing runs, and lingering movement sparks while moving. */
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
        try {
            if (!Boolean.TRUE.equals(isSwingInProgressMethod.invoke(minecraft))) {
                return false;
            }
            Object attackHand = getCurrentAttackHandMethod.invoke(minecraft);
            if (attackHand == null) {
                return false;
            }
            attackingStack = (ItemStack) attackHandItemStackMethod.invoke(attackHand);
        } catch (Throwable ignored) {
            return false;
        }
        if (!isArsenalWeapon(attackingStack)) {
            return false;
        }
        Level level = minecraft.level;
        if (level == null || bladeA == null || bladeB == null) {
            return false;
        }
        if (System.currentTimeMillis() - capturedAt > 500L) {
            // The item was not rendered recently (screen open, inventory, ...).
            return false;
        }
        WeaponGlowConfig.Trail trail = config.trail;
        ParticleOptions type = resolveParticle(trail.particle);
        GlowSparkParticle.nextScale = (float) trail.particleScale;
        GlowSparkParticle.nextLifetime = trail.particleLifetimeTicks;
        RandomSource random = RandomSource.create();
        for (int i = 0; i < trail.particlesPerTick; i++) {
            double t = random.nextDouble();
            Vec3 point = bladeA.lerp(bladeB, t);
            double x = point.x + (random.nextDouble() - 0.5D) * trail.spread * 2.0D;
            double y = point.y + (random.nextDouble() - 0.5D) * trail.spread * 2.0D;
            double z = point.z + (random.nextDouble() - 0.5D) * trail.spread * 2.0D;
            double vx = (random.nextDouble() - 0.5D) * trail.speed * 2.0D;
            double vy = (random.nextDouble() - 0.5D) * trail.speed * 2.0D;
            double vz = (random.nextDouble() - 0.5D) * trail.speed * 2.0D;
            level.addParticle(type, x, y, z, vx, vy, vz);
        }
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
        if (!holdingArsenalWeapon(player)) {
            return;
        }
        if (bladeA == null || bladeB == null || System.currentTimeMillis() - capturedAt > 500L) {
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
        ParticleOptions type = resolveParticle(config.trail.particle);
        GlowSparkParticle.nextScale = (float) motion.particleScale;
        GlowSparkParticle.nextLifetime = motion.particleLifetimeTicks;
        RandomSource random = RandomSource.create();
        for (int i = 0; i < motion.particlesPerTick; i++) {
            double t = random.nextDouble();
            Vec3 point = bladeA.lerp(bladeB, t);
            double x = point.x + (random.nextDouble() - 0.5D) * motion.spread * 2.0D;
            double y = point.y + (random.nextDouble() - 0.5D) * motion.spread * 2.0D;
            double z = point.z + (random.nextDouble() - 0.5D) * motion.spread * 2.0D;
            double vx = (random.nextDouble() - 0.5D) * motion.speed * 2.0D;
            double vy = (random.nextDouble() - 0.5D) * motion.speed * 2.0D;
            double vz = (random.nextDouble() - 0.5D) * motion.speed * 2.0D;
            level.addParticle(type, x, y, z, vx, vy, vz);
        }
    }

    private static ParticleOptions resolveParticle(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(location);
            if (type instanceof ParticleOptions options) {
                return options;
            }
        }
        return ParticleTypes.END_ROD;
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

    private static boolean holdingArsenalWeapon(LocalPlayer player) {
        return isArsenalWeapon(player.getMainHandItem()) || isArsenalWeapon(player.getOffhandItem());
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
