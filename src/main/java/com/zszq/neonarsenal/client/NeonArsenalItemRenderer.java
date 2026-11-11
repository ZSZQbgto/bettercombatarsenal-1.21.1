package com.zszq.neonarsenal.client;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zszq.neonarsenal.client.fx.WeaponGlow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Renders arsenal weapons context-dependently:
 *
 * <ul>
 *   <li>{@link ItemDisplayContext#GUI}: the supplied 16x16 thumbnail sprite
 *       ({@code item/<id>_thumbnail});</li>
 *   <li>everything else (first person, third person, ground, fixed, ...): the full 3D Blockbench
 *       model ({@code item/<id>_3d}).</li>
 * </ul>
 *
 * <p>The item model itself is a {@code builtin/entity} model carrying this weapon's display
 * transforms, so the vanilla item renderer applies the correct hand/ground transform before this
 * renderer is asked to draw. The renderer only has to draw quads, which is why the
 * {@code item/<id>_thumbnail} and {@code item/<id>_3d} models are registered as additional models
 * by {@link NeonArsenalClient}.</p>
 */
public final class NeonArsenalItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return NeonArsenalItemRenderer.get();
        }
    };

    private static NeonArsenalItemRenderer instance;
    private static final Map<Item, BakedModel> MODEL_CACHE = new IdentityHashMap<>();
    private static final Map<Item, BakedModel> THUMBNAIL_CACHE = new IdentityHashMap<>();
    private static final Map<Item, ResourceLocation> ID_CACHE = new IdentityHashMap<>();

    /** Drops all resolved models (called when the model manager reloads). */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
        THUMBNAIL_CACHE.clear();
        ID_CACHE.clear();
    }

    private NeonArsenalItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static NeonArsenalItemRenderer get() {
        if (instance == null) {
            instance = new NeonArsenalItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Item item = stack.getItem();
        ResourceLocation itemId = ID_CACHE.computeIfAbsent(item, key -> BuiltInRegistries.ITEM.getKey(key));

        // Blocking weapons swap to the authored first-person parry pose while the player parries.
        if (displayContext.firstPerson()) {
            FirstPersonParryPose.apply(stack, displayContext, poseStack, itemId);
        } else if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // During a Better Combat attack (including the manual falling-slash / charge-thrust
            // animations) the item swaps from the idle display transform to the attack-oriented one.
            ThirdPersonAttackPose.apply(stack, displayContext, poseStack, itemId);
        }

        boolean thumbnail = displayContext == ItemDisplayContext.GUI;
        Map<Item, BakedModel> cache = thumbnail ? THUMBNAIL_CACHE : MODEL_CACHE;
        BakedModel model = cache.get(item);
        if (model == null) {
            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
                    itemId.getNamespace(),
                    "item/" + itemId.getPath() + (thumbnail ? "_thumbnail" : "_3d"));
            model = Minecraft.getInstance().getModelManager()
                    .getModel(ModelResourceLocation.standalone(modelId));
            if (thumbnail && model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                // No supplied 16x16 icon for this weapon: fall back to the 3D model in the GUI.
                ResourceLocation fallbackId = ResourceLocation.fromNamespaceAndPath(
                        itemId.getNamespace(), "item/" + itemId.getPath() + "_3d");
                model = Minecraft.getInstance().getModelManager()
                        .getModel(ModelResourceLocation.standalone(fallbackId));
            }
            cache.put(item, model);
        }
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(
                bufferSource, Sheets.translucentItemSheet(), true, stack.hasFoil());
        Minecraft.getInstance().getItemRenderer()
                .renderModelLists(model, stack, packedLight, packedOverlay, poseStack, buffer);

        // The supplied "<id>_blade" masks are drawn as a full-bright overlay on top of the base
        // model, so only the authored blade pixels glow. The GUI keeps using the plain 2D icon.
        if (!thumbnail) {
            WeaponGlow.render(stack, itemId, displayContext, poseStack, bufferSource, packedLight);
        }
    }
}
