package com.zszq.neonarsenal.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.client.fx.FpaBend;
import com.zszq.neonarsenal.compat.AttackAnimationTracker;
import com.zszq.neonarsenal.weapon.HoldAnimationWeapons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;


/**
 * Authored main-hand holding pose for the arsenal weapons, driven by
 * {@code assets/bettercombat_neonarsenal/animations/weapon_holding_action_from_behind.json}.
 *
 * <p>The pose only drives the <b>main hand</b> (right arm by default, mirrored for left-handed
 * players); the off hand is never touched, so dual wielding keeps the offhand's own animation. It is
 * applied after all other pose/animation work, blended in and out over about 0.1 s, and fades out
 * while a Better Combat attack animation plays so the swing animations stay readable. While moving, a
 * small fore/back swing is added on top of the authored pose: sprinting swings around a 10 degree
 * backward offset with a 6 degree fan, walking around 5 degrees with a 3 degree fan, standing still
 * has no extra swing.</p>
 */
public final class HoldPose {

    private record Bone(float[] rotation, float[] position) {
    }

    private static final String ANIMATION = "weapon_holding_action_from_behind";
    private static final float BLEND_PER_SECOND = 10.0F;
    private static final float SWING_BLEND_PER_SECOND = 8.0F;
    /** Right arm elbow bend from the authored animation, in degrees. */
    private static final float BEND_X_DEGREES = -22.5F;
    /** Walk/sprint backward offset (mean) and fan (amplitude) in degrees. */
    private static final float SPRINT_CENTER = 10.0F;
    private static final float SPRINT_FAN = 8.0F;
    private static final float WALK_CENTER = 5.0F;
    private static final float WALK_FAN = 4.0F;

    private static final Map<UUID, Float> BLEND = new HashMap<>();
    private static final Map<UUID, float[]> SWING = new HashMap<>();
    private static final Map<PlayerModel<?>, float[]> BENDS = new WeakHashMap<>();
    private static Bone arm;
    private static boolean loadTried;

    private HoldPose() {
    }

    /** {@return whether the main-hand item uses this holding pose}. */
    public static boolean isHoldingPoseWeapon(ItemStack stack) {
        return HoldAnimationWeapons.isHoldPoseWeapon(stack);
    }

    /** {@return the current blend weight of this player, 0..1}. */
    public static float weight(AbstractClientPlayer player) {
        return player == null ? 0.0F : BLEND.getOrDefault(player.getUUID(), 0.0F);
    }

    /** Drops the elbow bend of the block-pose fallback, used while the block pose owns the arms. */
    public static void clear(PlayerModel<?> model) {
        BENDS.remove(model);
    }

    /** Advances the blend/swing state without applying anything (used while blocking fades in). */
    public static void decay(AbstractClientPlayer player) {
        float deltaSeconds = frameSeconds();
        float current = approach(weight(player), 0.0F, BLEND_PER_SECOND * clamp(deltaSeconds));
        if (current <= 0.001F) {
            BLEND.remove(player.getUUID());
        } else {
            BLEND.put(player.getUUID(), current);
        }
    }

    /**
     * Applies the authored holding pose to the main arm, blended by the current weight.
     *
     * @param limbSwing       vanilla limb swing position (walk animation phase)
     * @param limbSwingAmount vanilla limb swing amount (walk animation speed)
     */
    public static void apply(PlayerModel<?> model, AbstractClientPlayer player,
                             float limbSwing, float limbSwingAmount) {
        ensureLoaded();
        if (model == null || player == null || arm == null) {
            return;
        }
        float weight = update(player, limbSwingAmount);
        float[] swing = SWING.getOrDefault(player.getUUID(), new float[]{0.0F, 0.0F});
        if (weight <= 0.001F) {
            BENDS.remove(model);
            return;
        }
        boolean left = player.getMainArm() == HumanoidArm.LEFT;
        ModelPart armPart = left ? model.leftArm : model.rightArm;
        if (armPart == null) {
            return;
        }
        float[] rotation = arm.rotation() != null ? arm.rotation() : new float[]{0.0F, 0.0F, 0.0F};
        float swingDelta = (float) Math.toRadians(swing[0] + swing[1] * Math.sin(limbSwing * 0.6662F));
        float targetX = (float) Math.toRadians(rotation[0]) + swingDelta;
        float targetY = (float) Math.toRadians(rotation[1]);
        float targetZ = (float) Math.toRadians(rotation[2]);
        if (left) {
            targetY = -targetY;
            targetZ = -targetZ;
        }
        armPart.xRot = Mth.lerp(weight, armPart.xRot, targetX);
        armPart.yRot = Mth.lerp(weight, armPart.yRot, targetY);
        armPart.zRot = Mth.lerp(weight, armPart.zRot, targetZ);
        // The outer skin layers were copied from the animated arm earlier in setupAnim.
        if (left) {
            copyOuter(model.leftSleeve, model.leftArm);
        } else {
            copyOuter(model.rightSleeve, model.rightArm);
        }
        BENDS.put(model, new float[]{(float) Math.toRadians(BEND_X_DEGREES), weight, left ? 1.0F : 0.0F});
    }

    /** Applies the authored elbow bend right before the model renders (FancyPlayerAnimation hook). */
    public static void applyRenderBends(PlayerModel<?> model) {
        float[] bend = BENDS.get(model);
        if (bend == null || bend[1] <= 0.0F) {
            return;
        }
        if (bend[2] > 0.5F) {
            FpaBend.blend(model.leftArm, bend[0], bend[1]);
        } else {
            FpaBend.blend(model.rightArm, bend[0], bend[1]);
        }
    }

    /** Advances the blend weight and the walk-swing state; {@return the new blend weight}. */
    private static float update(AbstractClientPlayer player, float limbSwingAmount) {
        float deltaSeconds = frameSeconds();
        float current = weight(player);
        float target = eligible(player) ? 1.0F : 0.0F;
        current = approach(current, target, BLEND_PER_SECOND * clamp(deltaSeconds));
        if (current <= 0.001F) {
            BLEND.remove(player.getUUID());
        } else {
            BLEND.put(player.getUUID(), current);
        }

        boolean sprinting = player.isSprinting();
        boolean walking = !sprinting && (player.getDeltaMovement().horizontalDistance() > 0.03D
                || limbSwingAmount > 0.03F);
        float centerTarget = sprinting ? SPRINT_CENTER : walking ? WALK_CENTER : 0.0F;
        float fanTarget = sprinting ? SPRINT_FAN : walking ? WALK_FAN : 0.0F;
        float[] swing = SWING.getOrDefault(player.getUUID(), new float[]{0.0F, 0.0F});
        float swingStep = SWING_BLEND_PER_SECOND * clamp(deltaSeconds);
        swing[0] = approach(swing[0], centerTarget, swingStep);
        swing[1] = approach(swing[1], fanTarget, swingStep);
        if (Math.abs(swing[0]) <= 0.01F && Math.abs(swing[1]) <= 0.01F) {
            SWING.remove(player.getUUID());
        } else {
            SWING.put(player.getUUID(), swing);
        }
        return current;
    }

    private static boolean eligible(AbstractClientPlayer player) {
        if (!isHoldingPoseWeapon(player.getMainHandItem())) {
            return false;
        }
        if (player.isUsingItem() || player.isDeadOrDying() || player.isSleeping()
                || player.isSwimming() || player.onClimbable() || player.isFallFlying()) {
            return false;
        }
        if (player.swinging || AttackAnimationTracker.isAnimating(player)) {
            return false;
        }
        // The blocking pose owns the arms while it is active (including its fade-out), and the
        // charge thrust keeps its own manual attack animation on the arm for as long as it lasts.
        return BlockPose.progressOf(player) <= 0.001F
                && !ChargeThrustClient.isCharging();
    }

    private static void copyOuter(ModelPart outer, ModelPart inner) {
        if (outer != null && inner != null) {
            outer.copyFrom(inner);
        }
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(0.1F, value));
    }

    private static float frameSeconds() {
        return Minecraft.getInstance().getFrameTimeNs() / 1_000_000_000.0F;
    }

    private static void ensureLoaded() {
        if (loadTried) {
            return;
        }
        loadTried = true;
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    NeonArsenal.MODID, "animations/" + ANIMATION + ".json");
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
            if (resource == null) {
                return;
            }
            try (var reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null || animations.isEmpty()) {
                    return;
                }
                JsonObject animation = animations.entrySet().iterator().next().getValue().getAsJsonObject();
                JsonObject bones = animation.getAsJsonObject("bones");
                if (bones == null) {
                    return;
                }
                arm = bone(bones, "right_arm");
            }
        } catch (Throwable ignored) {
            // Missing/broken animation asset: keep the weapon's normal pose.
        }
    }

    private static Bone bone(JsonObject bones, String name) {
        JsonElement element = bones.get(name);
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject bone = element.getAsJsonObject();
        return new Bone(floats(bone.getAsJsonArray("rotation")), floats(bone.getAsJsonArray("position")));
    }

    private static float[] floats(com.google.gson.JsonArray array) {
        if (array == null || array.size() < 3) {
            return null;
        }
        return new float[]{
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        };
    }
}
