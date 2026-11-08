package com.zszq.neonarsenal.mixin;

import com.zszq.neonarsenal.client.BlockPose;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.PlayerModel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the blocking arm bend immediately before the model renders, after BC/FPA finished their
 * own setupAnim/bend pass. When no block pose is active this does nothing.
 */
@Mixin(AgeableListModel.class)
public abstract class BlockBendRenderMixin {

    @Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"))
    private void neonarsenal$applyBlockBend(CallbackInfo ci) {
        if ((Object) this instanceof PlayerModel<?> model) {
            BlockPose.applyRenderBends(model);
        }
    }
}
