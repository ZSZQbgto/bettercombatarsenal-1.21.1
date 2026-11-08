package com.zszq.neonarsenal.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client-side blocking pose driven only by the supplied one-frame parry animations.
 *
 * <p>The upper body (torso, head, hands) is smoothly blended to the animation file; legs and their
 * outer skin layers are left completely vanilla. The torso is re-attached to the waist after posing
 * so no gap can appear between the body and the legs.</p>
 */
public final class BlockPose {

    private record Bone(float[] rotation, float[] position) {
    }

    /** Weapons whose Better Combat attributes mark them as two-handed. */
    private static final Set<String> TWO_HANDED = Set.of(
            "claymore", "twinblade", "doubleaxe", "hengdao", "spear");

    private static final Map<UUID, Float> PROGRESS = new HashMap<>();
    private static final Map<PlayerModel<?>, float[]> RENDER_BENDS = new WeakHashMap<>();
    private static final Map<UUID, Float> LEG_BLEND = new HashMap<>();
    private static Map<String, Bone> twoHanded;
    private static Map<String, Bone> dualWield;
    private static boolean loadTried;

    private BlockPose() {
    }

    /** Advances the 0..1 pose blend for this frame; 10/s = 0.1 s block pose transition. */
    public static float progress(AbstractClientPlayer player, float deltaSeconds) {
        UUID id = player.getUUID();
        float target = isBlocking(player) ? 1.0F : 0.0F;
        float current = PROGRESS.getOrDefault(id, 0.0F);
        float step = 10.0F * Math.max(0.0F, Math.min(0.1F, deltaSeconds));
        if (current < target) {
            current = Math.min(target, current + step);
        } else if (current > target) {
            current = Math.max(target, current - step);
        }
        if (current <= 0.001F) {
            PROGRESS.remove(id);
            return 0.0F;
        }
        PROGRESS.put(id, current);
        return current;
    }

    /** {@return the current 0..1 block-pose blend for this player without advancing it}. */
    public static float progressOf(AbstractClientPlayer player) {
        return player == null ? 0.0F : PROGRESS.getOrDefault(player.getUUID(), 0.0F);
    }

    public static boolean isBlocking(Player player) {
        return player != null
                && !player.isFallFlying()
                && player.isUsingItem()
                && player.getUseItem().getItem() instanceof SharpnessWeaponItem weapon
                && weapon.canBlock();
    }

    /** Applies the blocking pose to the upper body; legs are never touched. */
    public static void apply(PlayerModel<?> model, AbstractClientPlayer player, float progress) {
        ensureLoaded();
        boolean mainTwoHanded = isTwoHanded(player.getMainHandItem());
        boolean dual = !mainTwoHanded && player.getOffhandItem().getItem() instanceof SharpnessWeaponItem;
        Map<String, Bone> bones = dual ? dualWield : twoHanded;
        if (bones == null) {
            return;
        }

        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float armBaseY = slim ? 2.5F : 2.0F;
        // Only the file drives these bones; at full progress other animations are fully replaced.
        setBone(model.body, bones.get("torso"), 0.0F, 0.0F, 0.0F, progress);
        setBone(model.head, bones.get("head"), 0.0F, 0.0F, 0.0F, progress);
        setBone(model.rightArm, bones.get("right_arm"), -5.0F, armBaseY, 0.0F, progress);
        setBone(model.leftArm, bones.get("left_arm"), 5.0F, armBaseY, 0.0F, progress);

        attachTorsoToLegs(model.body, progress);
        Quaternionf bodyRotation = new Quaternionf()
                .rotateZ(model.body.zRot).rotateY(model.body.yRot).rotateX(model.body.xRot);
        attachHead(model.head, model.body, progress);
        followBody(model.rightArm, -5.0F, armBaseY, bones.get("right_arm"),
                model.body, bodyRotation, progress);
        followBody(model.leftArm, 5.0F, armBaseY, bones.get("left_arm"),
                model.body, bodyRotation, progress);

        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.jacket.copyFrom(model.body);

        // Block -> sneak transition: vanilla switches the legs to the sneaking position at once
        // while the upper body still blends for 0.1 s. Lerp the legs by the same progress so they
        // move together with the body. (Only active while the player actually crouches again.)
        if (model.crouching && progress < 1.0F) {
            float sneak = 1.0F - progress;
            model.rightLeg.z = 4.0F * sneak;
            model.leftLeg.z = 4.0F * sneak;
            model.rightLeg.y = 12.0F + 0.2F * sneak;
            model.leftLeg.y = 12.0F + 0.2F * sneak;
        }

        // Stationary file-leg pose vs. moving vanilla-walk pose blends over 0.1 s, same as the
        // block pose blend.
        boolean stationary = player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D
                && player.walkAnimation.speed() < 0.02F;
        boolean wantFileLegs = stationary && !model.crouching;
        float deltaSeconds = Minecraft.getInstance().getFrameTimeNs() / 1_000_000_000.0F;
        float legTarget = wantFileLegs ? 1.0F : 0.0F;
        float legBlend = LEG_BLEND.getOrDefault(player.getUUID(), 0.0F);
        float legStep = 10.0F * Math.max(0.0F, Math.min(0.1F, deltaSeconds));
        legBlend += Math.max(-legStep, Math.min(legStep, legTarget - legBlend));
        if (legBlend <= 0.001F) {
            LEG_BLEND.remove(player.getUUID());
            legBlend = 0.0F;
        } else {
            LEG_BLEND.put(player.getUUID(), legBlend);
        }

        // The block pose itself uses the 0.1 s transition; multiply it into the file-leg weight so
        // idle <-> block and moving <-> stationary leg changes both finish in 0.1 s.
        float fileLegWeight = legBlend * progress;
        if (fileLegWeight > 0.0F) {
            setBone(model.rightLeg, bones.get("right_leg"), -1.9F, 12.0F, 0.0F, fileLegWeight);
            setBone(model.leftLeg, bones.get("left_leg"), 1.9F, 12.0F, 0.0F, fileLegWeight);
            // The authored leg position has z=+2, which pulls the leg roots behind the body; keep
            // the roots under the body so the legs stay attached to the torso.
            model.rightLeg.z = lerp(model.rightLeg.z, 0.0F, fileLegWeight);
            model.leftLeg.z = lerp(model.leftLeg.z, 0.0F, fileLegWeight);
        } else if (!model.crouching) {
            // Vanilla setupAnim already computed the walking rotations; only reset positions left
            // over from the file pose.
            model.rightLeg.x = -1.9F;
            model.leftLeg.x = 1.9F;
            model.rightLeg.y = 12.0F;
            model.leftLeg.y = 12.0F;
            model.rightLeg.z = 0.0F;
            model.leftLeg.z = 0.0F;
        }
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);

        // Lower the whole blocking pose so the authored file-leg soles touch the ground (model
        // bottom = y 24). The offset follows the file-leg pose; while walking the vanilla legs swing
        // and a full offset would make the whole torso bob up and down, so weight it by the file-leg
        // blend: stationary blocking is grounded, walking keeps the torso steady.
        float footOffset = groundOffset(model) * fileLegWeight;
        if (Math.abs(footOffset) > 0.001F) {
            shiftY(model.body, footOffset);
            shiftY(model.head, footOffset);
            shiftY(model.leftArm, footOffset);
            shiftY(model.rightArm, footOffset);
            shiftY(model.leftLeg, footOffset);
            shiftY(model.rightLeg, footOffset);
            shiftY(model.leftSleeve, footOffset);
            shiftY(model.rightSleeve, footOffset);
            shiftY(model.jacket, footOffset);
            shiftY(model.leftPants, footOffset);
            shiftY(model.rightPants, footOffset);
        }

        RENDER_BENDS.put(model, new float[]{bendAngle(bones.get("right_arm_bend"), progress),
                bendAngle(bones.get("left_arm_bend"), progress), progress});
    }

    /** {@return the vertical shift needed so the rotated leg soles sit on model y=24}. */
    private static float groundOffset(PlayerModel<?> model) {
        float bottom = Math.min(legBottom(model.rightLeg), legBottom(model.leftLeg));
        return 24.0F - bottom;
    }

    private static float legBottom(ModelPart leg) {
        if (leg == null) {
            return 24.0F;
        }
        float cos = (float) Math.cos(leg.xRot);
        float sin = Math.abs((float) Math.sin(leg.xRot));
        // Leg cube is 12 units long and 4 units wide; lowest corner of the sole.
        return leg.y + 12.0F * cos - 2.0F * sin;
    }

    private static void shiftY(ModelPart part, float offset) {
        if (part != null) {
            part.y += offset;
        }
    }

    private static float bendAngle(Bone bendBone, float progress) {
        if (bendBone == null || bendBone.rotation() == null) {
            return 0.0F;
        }
        // Positive authored X angle (the previous attempt used the negated angle and bent backwards).
        return (float) Math.toRadians(bendBone.rotation()[0]) * progress;
    }

    public static void clearRenderBends(PlayerModel<?> model) {
        RENDER_BENDS.remove(model);
    }

    public static void applyRenderBends(PlayerModel<?> model) {
        float[] bend = RENDER_BENDS.get(model);
        if (bend == null || bend[2] <= 0.0F) {
            return;
        }
        com.zszq.neonarsenal.client.fx.FpaBend.blend(model.rightArm, bend[0], bend[2]);
        com.zszq.neonarsenal.client.fx.FpaBend.blend(model.leftArm, bend[1], bend[2]);
        com.zszq.neonarsenal.client.fx.FpaBend.blend(model.rightSleeve, bend[0], bend[2]);
        com.zszq.neonarsenal.client.fx.FpaBend.blend(model.leftSleeve, bend[1], bend[2]);
    }

    private static void setBone(ModelPart part, Bone bone, float baseX, float baseY, float baseZ,
                                float progress) {
        if (part == null) {
            return;
        }
        float targetX = bone != null && bone.rotation() != null
                ? (float) Math.toRadians(bone.rotation()[0]) : 0.0F;
        float targetY = bone != null && bone.rotation() != null
                ? (float) Math.toRadians(bone.rotation()[1]) : 0.0F;
        float targetZ = bone != null && bone.rotation() != null
                ? (float) Math.toRadians(bone.rotation()[2]) : 0.0F;
        part.xRot = lerp(part.xRot, targetX, progress);
        part.yRot = lerp(part.yRot, targetY, progress);
        part.zRot = lerp(part.zRot, targetZ, progress);

        float posX = bone != null && bone.position() != null ? bone.position()[0] : 0.0F;
        float posY = bone != null && bone.position() != null ? bone.position()[1] : 0.0F;
        float posZ = bone != null && bone.position() != null ? bone.position()[2] : 0.0F;
        part.x = lerp(part.x, baseX + posX, progress);
        part.y = lerp(part.y, baseY + posY, progress);
        part.z = lerp(part.z, baseZ + posZ, progress);
    }

    /** Re-attaches the rotated torso's bottom (local y=12) to the leg pivot so no gap can appear. */
    private static void attachTorsoToLegs(ModelPart body, float progress) {
        if (body == null || progress <= 0.0F) {
            return;
        }
        Quaternionf q = new Quaternionf().rotateZ(body.zRot).rotateY(body.yRot).rotateX(body.xRot);
        Vector3f bottom = new Vector3f(0.0F, 12.0F, 0.0F).rotate(q);
        bottom.add(body.x, body.y, body.z);
        Vector3f delta = new Vector3f(-bottom.x, 12.0F - bottom.y, -bottom.z).mul(progress);
        body.x += delta.x;
        body.y += delta.y;
        body.z += delta.z;
    }

    /**
     * Keeps the head pivot on the torso's top pivot, lowered slightly so the head visibly touches
     * the body instead of floating above it.
     */
    private static void attachHead(ModelPart head, ModelPart body, float progress) {
        if (head == null || body == null) {
            return;
        }
        Vector3f target = new Vector3f(0.0F, 0.0F, 0.0F);
        target.add(body.x, body.y, body.z);
        target.y += 0.5F;
        head.x = lerp(head.x, target.x, progress);
        head.y = lerp(head.y, target.y, progress);
        head.z = lerp(head.z, target.z, progress);
    }

    /**
     * Places an arm pivot at {@code bodyTranslation + bodyRotation * localPivot}, so it keeps its
     * authored offset relative to the torso while the torso rotates and attaches to the legs.
     */
    private static void followBody(ModelPart part, float baseX, float baseY, Bone bone,
                                   ModelPart body, Quaternionf bodyRotation, float progress) {
        if (part == null || body == null) {
            return;
        }
        float posX = bone != null && bone.position() != null ? bone.position()[0] : 0.0F;
        float posY = bone != null && bone.position() != null ? bone.position()[1] : 0.0F;
        float posZ = bone != null && bone.position() != null ? bone.position()[2] : 0.0F;
        Vector3f local = new Vector3f(baseX + posX, baseY + posY, posZ).rotate(bodyRotation);
        local.add(body.x, body.y, body.z);
        part.x = lerp(part.x, local.x, progress);
        part.y = lerp(part.y, local.y, progress);
        part.z = lerp(part.z, local.z, progress);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static boolean isTwoHanded(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && TWO_HANDED.contains(id.getPath());
    }

    private static void ensureLoaded() {
        if (loadTried) {
            return;
        }
        twoHanded = load("two_handed_parry");
        dualWield = load("dual_wield_parry");
        loadTried = twoHanded != null && dualWield != null;
    }

    private static Map<String, Bone> load(String name) {
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    NeonArsenal.MODID, "animations/" + name + ".json");
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
            if (resource == null) {
                return null;
            }
            try (var reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null || animations.isEmpty()) {
                    return null;
                }
                JsonObject animation = animations.entrySet().iterator().next().getValue().getAsJsonObject();
                JsonObject bones = animation.getAsJsonObject("bones");
                if (bones == null) {
                    return null;
                }
                Map<String, Bone> result = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
                    JsonObject bone = entry.getValue().getAsJsonObject();
                    result.put(entry.getKey(), new Bone(
                            floats(bone.getAsJsonArray("rotation")),
                            floats(bone.getAsJsonArray("position"))));
                }
                return result;
            }
        } catch (Throwable ignored) {
            return null;
        }
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
