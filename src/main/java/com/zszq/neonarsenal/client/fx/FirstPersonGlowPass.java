package com.zszq.neonarsenal.client.fx;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Iris adaptation for the first-person blade glow.
 *
 * <p>Iris renders first-person hand items in their own g-buffer pass, where blending is controlled
 * by the shader pack: an additive overlay drawn from inside the hand renderer is either forced to
 * that pass' blend state (the blade turns flat grey) or ends up behind the hand model. Instead, the
 * overlay is drawn <b>after</b> the hand has been rendered (during the GUI pre phase) with the exact
 * matrices and item pose that were captured while each hand was drawn, using the normal additive,
 * full-bright layer. Main hand and offhand are tracked separately so both glow.</p>
 */
public final class FirstPersonGlowPass {

    private static final Slot MAIN_HAND = new Slot();
    private static final Slot OFF_HAND = new Slot();

    private FirstPersonGlowPass() {
    }

    /** Called from the item renderer while a first-person arsenal weapon is drawn. */
    public static void capture(ItemDisplayContext context, BakedModel overlay, PoseStack poseStack,
                               float[] color, float opacity) {
        Slot slot = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? OFF_HAND : MAIN_HAND;
        slot.model = overlay;
        slot.itemPose.set(poseStack.last().pose());
        slot.modelView.set(RenderSystem.getModelViewMatrix());
        slot.projection.set(RenderSystem.getProjectionMatrix());
        slot.tint = color.clone();
        slot.alpha = opacity;
        slot.pending = true;
    }

    /** Draws the captured overlays right after the hand rendering, before the GUI is drawn. */
    public static void render(RenderGuiEvent.Pre event) {
        boolean any = MAIN_HAND.pending || OFF_HAND.pending;
        if (!any) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            draw(minecraft, MAIN_HAND);
            draw(minecraft, OFF_HAND);
        }
        MAIN_HAND.pending = false;
        OFF_HAND.pending = false;
    }

    private static void draw(Minecraft minecraft, Slot slot) {
        if (!slot.pending || slot.model == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType layer = WeaponGlow.deferredLayer();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderSystem.setProjectionMatrix(slot.projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.getModelViewStack().mul(slot.modelView);
        RenderSystem.applyModelViewMatrix();

        PoseStack pose = new PoseStack();
        pose.last().pose().set(slot.itemPose);
        pose.last().normal().set(new Matrix3f(slot.itemPose).invert().transpose());

        VertexConsumer buffer = buffers.getBuffer(layer);
        WeaponGlow.drawFlowingOverlayQuads(buffer, pose.last(), slot.model, slot.tint, slot.alpha,
                false, LightTexture.FULL_BRIGHT);
        buffers.endBatch(layer);

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.DISTANCE_TO_ORIGIN);
    }

    private static final class Slot {
        private boolean pending;
        private BakedModel model;
        private final Matrix4f itemPose = new Matrix4f();
        private final Matrix4f modelView = new Matrix4f();
        private final Matrix4f projection = new Matrix4f();
        private float[] tint = new float[]{1.0F, 1.0F, 1.0F};
        private float alpha = 1.0F;
    }
}
