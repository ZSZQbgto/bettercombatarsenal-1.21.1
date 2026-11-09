package com.zszq.neonarsenal.client.fx;

import com.zszq.neonarsenal.client.BlockPose;
import com.zszq.neonarsenal.compat.BlockSparkOptions;
import com.zszq.neonarsenal.item.Forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * First-person spark burst for a successful block.
 *
 * <p>The server emits the world-space burst at the weapon position, which is barely visible from
 * the blocking player's own eye. This adds a local burst in front of the camera, anchored near the
 * held weapon, using the same particle type, weapon colour, glow flag and density as the world
 * burst, so the first-person parry gets the same feedback.</p>
 */
public final class FirstPersonBlockBurst {

    private static final int COUNT = 14;
    private static final double SPREAD = 0.25D;

    private FirstPersonBlockBurst() {
    }

    /** Called when the client receives the parry sound; only bursts for the local blocker. */
    public static void trySpawn(double soundX, double soundY, double soundZ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!BlockPose.isBlocking(player)) {
            return;
        }
        // The sound is played at the blocker's own block position; ignore parries from other players.
        if (player.position().distanceToSqr(soundX, soundY, soundZ) > 9.0D) {
            return;
        }
        ItemStack weapon = player.getUseItem();
        BlockSparkOptions options = new BlockSparkOptions(
                Forge.displayColor(weapon), Forge.glowEnabled(weapon));
        RandomSource random = player.getRandom();
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        // Anchor near the lower right of the first-person view, where the parry blade sits.
        Vec3 base = player.getEyePosition()
                .add(look.scale(0.55D))
                .add(right.scale(0.30D))
                .add(0.0D, -0.28D, 0.0D);
        for (int i = 0; i < COUNT; i++) {
            double x = base.x + (random.nextDouble() * 2.0D - 1.0D) * SPREAD * 0.35D;
            double y = base.y + (random.nextDouble() * 2.0D - 1.0D) * SPREAD * 0.35D;
            double z = base.z + (random.nextDouble() * 2.0D - 1.0D) * SPREAD * 0.35D;
            double vx = look.x * 0.10D + (random.nextDouble() * 2.0D - 1.0D) * 0.18D;
            double vy = look.y * 0.10D + (random.nextDouble() * 2.0D - 1.0D) * 0.18D;
            double vz = look.z * 0.10D + (random.nextDouble() * 2.0D - 1.0D) * 0.18D;
            level.addParticle(options, x, y, z, vx, vy, vz);
        }
    }
}
