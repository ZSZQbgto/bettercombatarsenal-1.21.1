package com.zszq.neonarsenal.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszq.neonarsenal.client.BlockPose;
import com.zszq.neonarsenal.client.fx.FpaBend;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.PlayerModelPart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the cape anchored to the posed torso during the blocking pose when FancyPlayerAnimation is
 * not installed.
 *
 * <p>Vanilla {@code CapeLayer} draws the cape at the player model origin. The blocking pose rotates
 * the torso around the waist and re-attaches it to the legs, so the vanilla anchor no longer sits on
 * the shoulders' back edge and the cape visibly floats away from the body. This mixin applies the
 * current body-part transform for the duration of the cape render, so the cape follows the back edge
 * of the shoulders exactly like the arms and head do. With FancyPlayerAnimation installed FPA drives
 * the cape already, so this transform is skipped.</p>
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerBlockPoseMixin {

    @Unique
    private boolean neonarsenal$capePushed;

    @Inject(method = "render", at = @At("HEAD"))
    private void neonarsenal$attachCapeToPosedTorso(PoseStack poseStack, MultiBufferSource buffer,
                                                    int packedLight, AbstractClientPlayer player,
                                                    float limbSwing, float limbSwingAmount,
                                                    float partialTicks, float ageInTicks,
                                                    float netHeadYaw, float headPitch, CallbackInfo ci) {
        neonarsenal$capePushed = false;
        if (player == null || player.isInvisible()
                || !player.isModelPartShown(PlayerModelPart.CAPE)) {
            return;
        }
        // FancyPlayerAnimation already attaches the cape to its animated body.
        if (FpaBend.available() || BlockPose.progressOf(player) <= 0.001F) {
            return;
        }
        EntityModel<?> parent = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (!(parent instanceof PlayerModel<?> model)) {
            return;
        }
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        neonarsenal$capePushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neonarsenal$detachCape(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                        AbstractClientPlayer player, float limbSwing,
                                        float limbSwingAmount, float partialTicks, float ageInTicks,
                                        float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (neonarsenal$capePushed) {
            neonarsenal$capePushed = false;
            poseStack.popPose();
        }
    }
}
