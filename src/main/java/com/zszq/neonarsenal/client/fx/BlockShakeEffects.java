package com.zszq.neonarsenal.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Short hand/weapon and camera shake played when a parry actually triggers. */
public final class BlockShakeEffects {

    private static final RandomSource RANDOM = RandomSource.create();
    private static int ticks;

    private BlockShakeEffects() {
    }

    public static void trigger() {
        ticks = 2; // 0.1 s
    }

    public static void tick() {
        if (ticks > 0) {
            ticks--;
        }
    }

    /** Shakes the first-person hand and the weapon it holds by roughly two pixels. */
    public static void onRenderHand(RenderHandEvent event) {
        if (ticks <= 0) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        // 2 pixels in item space (1 block = 16 px).
        pose.translate(jitter() * (2.0F / 16.0F), jitter() * (2.0F / 16.0F), 0.0F);
    }

    /** Small camera shake while the parry recoil is active. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticks <= 0) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()) {
            return;
        }
        event.setYaw(event.getYaw() + jitter() * 0.7F);
        event.setPitch(event.getPitch() + jitter() * 0.7F);
        event.setRoll(event.getRoll() + jitter() * 0.5F);
    }

    private static float jitter() {
        return RANDOM.nextFloat() * 2.0F - 1.0F;
    }
}
