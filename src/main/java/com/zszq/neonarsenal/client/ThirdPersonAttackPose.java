package com.zszq.neonarsenal.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.compat.AttackAnimationTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Third-person attack item pose.
 *
 * <p>The {@code bctppitemholding} exports contain an attack-oriented
 * {@code thirdperson_righthand} display transform (hand-aligned) for every weapon whose idle
 * orientation differs. While the rendered player is playing a Better Combat attack animation (normal
 * attacks, plus the local falling slash / charge thrust manual animations), the item renderer is
 * already inside the normal idle display transform, so this class swaps it for the attack transform
 * by applying {@code Centre^-1 * Normal^-1 * Attack * Centre}. The weapon mesh itself is unchanged.</p>
 *
 * <p>The swap fades in over 0.10 s when the attack starts and out over 0.15 s when it ends. The
 * rendered entity is captured by {@link com.zszq.neonarsenal.mixin.ItemInHandLayerContextMixin}, so
 * the pose also applies to other players in multiplayer. Only the authored right-hand attack
 * transform exists, so left-hand/offhand rendering keeps the existing idle placement.</p>
 */
public final class ThirdPersonAttackPose {

    private static final float FADE_IN_SECONDS = 0.10F;
    private static final float FADE_OUT_SECONDS = 0.15F;

    /**
     * The vanilla item renderer centres every model with {@code translate(-0.5, -0.5, -0.5)} after
     * the display transform, so that offset is conjugated out before swapping the transform, exactly
     * like the first-person parry pose.
     */
    private static final Matrix4f ITEM_CENTRE = new Matrix4f().translate(-0.5F, -0.5F, -0.5F);
    private static final Map<UUID, Float> WEIGHTS = new HashMap<>();
    private static final Map<ResourceLocation, Correction> CORRECTIONS = new HashMap<>();
    /** Reused output of the per-frame blended quaternion. */
    private static final Quaternionf POSE_ROTATION = new Quaternionf();

    private static LivingEntity renderedEntity;

    private ThirdPersonAttackPose() {
    }

    private record Correction(float x, float y, float z, Quaternionf rotation) {
    }

    /** Called by the inventory/held-item layer mixin before the entity's items are rendered. */
    public static void beginEntity(LivingEntity entity) {
        renderedEntity = entity;
    }

    /** Called by the inventory/held-item layer mixin after the entity's items were rendered. */
    public static void endEntity() {
        renderedEntity = null;
    }

    /** Called from {@link NeonArsenalItemRenderer} for third-person display contexts. */
    public static void apply(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             ResourceLocation itemId) {
        if (displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // Only the right-hand attack transform is authored; the left hand keeps its idle pose.
            return;
        }
        if (!(renderedEntity instanceof Player player) || stack.isEmpty()) {
            return;
        }
        boolean attacking = AttackAnimationTracker.isAnimating(player)
                || (player == Minecraft.getInstance().player
                && (FallSlamAnimation.isControlling() || ChargeThrustAnimation.isControlling()));
        float weight = updateWeight(player.getUUID(), attacking);
        if (weight <= 0.0005F) {
            return;
        }
        Correction correction = correction(stack, itemId, player);
        if (correction == null) {
            return;
        }
        poseStack.translate(correction.x * weight, correction.y * weight, correction.z * weight);
        Quaternionf rotation = correction.rotation;
        POSE_ROTATION.set(rotation.x * weight, rotation.y * weight, rotation.z * weight,
                rotation.w * weight + (1.0F - weight)).normalize();
        poseStack.mulPose(POSE_ROTATION);
    }

    /** {@return the cached attack correction, or {@code null} when the attack model is unavailable}. */
    private static Correction correction(ItemStack stack, ResourceLocation itemId, Player player) {
        Correction correction = CORRECTIONS.get(itemId);
        if (correction != null) {
            return correction;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel normal = minecraft.getItemRenderer().getModel(stack, player.level(), player, player.getId());
        ResourceLocation attackId = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath() + "_tppattack");
        BakedModel attack = minecraft.getModelManager()
                .getModel(ModelResourceLocation.standalone(attackId));
        if (attack == minecraft.getModelManager().getMissingModel()) {
            return null;
        }
        ItemDisplayContext context = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        // Swap the idle display transform for the attack one in the item's own (centred) space:
        // pose * Centre^-1 * Normal^-1 * Attack * Centre.
        Matrix4f matrix = new Matrix4f(ITEM_CENTRE).invert()
                .mul(transform(normal, context).invert())
                .mul(transform(attack, context))
                .mul(ITEM_CENTRE);
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
        correction = new Correction(translation.x, translation.y, translation.z, rotation);
        CORRECTIONS.put(itemId, correction);
        return correction;
    }

    private static Matrix4f transform(BakedModel model, ItemDisplayContext displayContext) {
        PoseStack poseStack = new PoseStack();
        ItemTransform transform = model.getTransforms().getTransform(displayContext);
        transform.apply(false, poseStack);
        return new Matrix4f(poseStack.last().pose());
    }

    /** Advances the fade weight once per rendered player; the frame delta is shared between calls. */
    private static float updateWeight(UUID id, boolean active) {
        // Minecraft's frame time is the same for every call in a frame, so multiple rendered players
        // advance by the same delta instead of competing for it.
        float frameDelta = Math.min(0.1F,
                Math.max(0.0F, Minecraft.getInstance().getFrameTimeNs() / 1.0E9F));
        float target = active ? 1.0F : 0.0F;
        float step = frameDelta / (active ? FADE_IN_SECONDS : FADE_OUT_SECONDS);
        float current = WEIGHTS.getOrDefault(id, 0.0F);
        current += Math.max(-step, Math.min(step, target - current));
        if (current <= 0.0005F) {
            WEIGHTS.remove(id);
            return 0.0F;
        }
        WEIGHTS.put(id, current);
        return current;
    }

    /** Drops the cached corrections (called when the model manager reloads). */
    public static void clearCorrectionCache() {
        CORRECTIONS.clear();
    }

    /** Drops every tracked player weight and cache (used on disconnect). */
    public static void clearAll() {
        WEIGHTS.clear();
        CORRECTIONS.clear();
        renderedEntity = null;
    }
}
