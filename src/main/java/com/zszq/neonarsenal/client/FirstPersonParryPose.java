package com.zszq.neonarsenal.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * First-person parry pose.
 *
 * <p>The supplied {@code resourcepacks/firstpersonparry} files carry a dedicated
 * {@code firstperson_righthand}/{@code firstperson_lefthand} display transform for blocking. They are
 * packaged as the additional models {@code item/<id>_fpparry}. While the local player blocks with the
 * weapon, the item renderer is already inside the normal first-person hand transform, so this class
 * swaps it for the parry transform by applying {@code inverse(normal) * parry} on top of the current
 * pose. Entering and leaving the block eases in and out with a smoothstep curve, so the swap between
 * the normal grip and the parry pose has no visible snap.</p>
 */
public final class FirstPersonParryPose {

    /** Full transition time between the normal grip and the parry pose. */
    private static final float FADE_SECONDS = 0.18F;
    /**
     * The authored first-person parry display transform was designed against BlockBench's hand
     * anchor, which sits higher than the vanilla first-person hand origin, so the raw transform puts
     * the blade at the very bottom of the screen. This view-space lift (in blocks) brings it up into
     * the lower half of the view.
     */
    private static final float VIEW_LIFT = 0.22F;
    /**
     * The vanilla item renderer centres every item model with {@code translate(-0.5, -0.5, -0.5)}
     * after the display transform, so that offset has to be conjugated out before swapping the
     * transform, otherwise the correction would be applied in a rotated frame and push the weapon
     * off-screen.
     */
    private static final Matrix4f ITEM_CENTRE = new Matrix4f().translate(-0.5F, -0.5F, -0.5F);
    private static final Map<UUID, Float> PROGRESS = new HashMap<>();
    /** Cached {@code Centre^-1 * Normal^-1 * Lift * Parry * Centre} per item and hand. */
    private static final Map<ResourceLocation, Correction> RIGHT_CORRECTIONS = new HashMap<>();
    private static final Map<ResourceLocation, Correction> LEFT_CORRECTIONS = new HashMap<>();
    /** Reused output of the per-frame blended quaternion. */
    private static final Quaternionf POSE_ROTATION = new Quaternionf();
    private static long lastNanos;

    private record Correction(float x, float y, float z, Quaternionf rotation) {
    }

    private FirstPersonParryPose() {
    }

    /** Called from {@link NeonArsenalItemRenderer} for first-person display contexts. */
    public static void apply(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             ResourceLocation itemId) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean mainHandBlocking = BlockPose.isBlocking(player)
                && ItemStack.isSameItemSameComponents(player.getUseItem(), stack);
        // Dual-wield parry: the blocking weapon is in the main hand while the off hand holds another
        // arsenal weapon. The off-hand blade then uses its own authored first-person parry transform.
        boolean offHandBlocking = !mainHandBlocking
                && BlockPose.isDualWieldBlock(player)
                && ItemStack.isSameItemSameComponents(player.getOffhandItem(), stack);
        boolean blocking = mainHandBlocking || offHandBlocking;
        float weight = ease(updateProgress(player.getUUID(), blocking));
        if (weight <= 0.0005F) {
            return;
        }
        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        Correction correction = correction(stack, displayContext, itemId, leftHand, minecraft, player);
        if (correction == null) {
            return;
        }
        poseStack.translate(correction.x * weight, correction.y * weight, correction.z * weight);
        Quaternionf rotation = correction.rotation;
        // Reuse one scratch quaternion instead of allocating a blended copy every frame.
        POSE_ROTATION.set(rotation.x * weight, rotation.y * weight, rotation.z * weight,
                rotation.w * weight + (1.0F - weight)).normalize();
        poseStack.mulPose(POSE_ROTATION);
    }

    /** {@return the cached parry correction, or {@code null} when the parry model is unavailable}. */
    private static Correction correction(ItemStack stack, ItemDisplayContext displayContext,
                                         ResourceLocation itemId, boolean leftHand, Minecraft minecraft,
                                         LocalPlayer player) {
        Map<ResourceLocation, Correction> cache = leftHand ? LEFT_CORRECTIONS : RIGHT_CORRECTIONS;
        Correction correction = cache.get(itemId);
        if (correction != null) {
            return correction;
        }
        BakedModel normal = minecraft.getItemRenderer().getModel(stack, player.level(), player, player.getId());
        ResourceLocation parryId = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath() + "_fpparry");
        BakedModel parry = minecraft.getModelManager()
                .getModel(ModelResourceLocation.standalone(parryId));
        if (parry == minecraft.getModelManager().getMissingModel()) {
            return null;
        }
        if (leftHand) {
            // Only weapons with an authored *left-hand* parry transform get the off-hand pose. Weapons
            // whose fpparry left-hand transform equals the normal one (for example the two-handed
            // weapons) must not receive the view lift, which would otherwise move their off-hand
            // rendering up even though no parry pose was authored for it.
            ItemDisplayContext leftContext = ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            if (normal.getTransforms().getTransform(leftContext)
                    .equals(parry.getTransforms().getTransform(leftContext))) {
                return null;
            }
        }
        // Replace the normal display transform with the parry one, in the item's own (centred)
        // space: pose * Centre^-1 * Normal^-1 * Lift * Parry * Centre.
        Matrix4f matrix = new Matrix4f(ITEM_CENTRE).invert()
                .mul(transform(normal, displayContext, leftHand).invert())
                .mul(new Matrix4f().translate(0.0F, VIEW_LIFT, 0.0F))
                .mul(transform(parry, displayContext, leftHand))
                .mul(ITEM_CENTRE);
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
        correction = new Correction(translation.x, translation.y, translation.z, rotation);
        cache.put(itemId, correction);
        return correction;
    }

    /** Drops the cached parry corrections (called when models are reloaded). */
    public static void clearCorrectionCache() {
        RIGHT_CORRECTIONS.clear();
        LEFT_CORRECTIONS.clear();
    }

    private static Matrix4f transform(BakedModel model, ItemDisplayContext displayContext, boolean leftHand) {
        PoseStack poseStack = new PoseStack();
        ItemTransform transform = model.getTransforms().getTransform(displayContext);
        transform.apply(leftHand, poseStack);
        return new Matrix4f(poseStack.last().pose());
    }

    /** Smoothstep so both ends of the transition start and stop at zero speed. */
    private static float ease(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    /** Drops the blend progress of every player (used on disconnect). */
    public static void clearAll() {
        PROGRESS.clear();
        clearCorrectionCache();
    }

    private static float updateProgress(UUID id, boolean blocking) {
        long now = System.nanoTime();
        float delta = lastNanos == 0L ? 0.05F : Mth.clamp((now - lastNanos) / 1.0E9F, 0.0F, 0.1F);
        lastNanos = now;
        float current = PROGRESS.getOrDefault(id, 0.0F);
        float step = delta / FADE_SECONDS;
        float next = blocking ? Math.min(1.0F, current + step) : Math.max(0.0F, current - step);
        if (next <= 0.0F) {
            PROGRESS.remove(id);
        } else {
            PROGRESS.put(id, next);
        }
        return next;
    }
}
