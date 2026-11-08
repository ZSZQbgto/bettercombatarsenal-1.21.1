package com.zszq.neonarsenal.compat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.registry.ModItems;
import com.zszq.neonarsenal.registry.ModSounds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Independent server-side falling-slash (下落斩击) settlement. The client sends
 * {@link FallSlamPayload} once the slash commits (four blocks above ground); the server then applies
 * the landing damage, fall-damage rules and the blaze-powder particle burst without relying on
 * Better Combat attacks.
 */
public final class FallSlamHandler {

    private static final Map<UUID, Float> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LANDED_FALL = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> FALL_IMPACT = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> SLAM_ACTIVE = ConcurrentHashMap.newKeySet();
    private static final java.util.Set<UUID> WINDUP_ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> WINDUP_GROUNDED = new ConcurrentHashMap<>();

    /** Safety net: if the client never restores the flag (crash, stuck state), do it after 5 s. */
    private static final int MAX_GROUNDED_WINDUP_TICKS = 100;

    private FallSlamHandler() {
    }

    /** Disables/enables Better Combat's server-side attack channel while the fall windup holds. */
    public static void setWindup(ServerPlayer player, boolean windup) {
        try {
            net.bettercombat.api.CombatFlags.setAttacksDisabled(player, windup);
            if (windup) {
                WINDUP_ACTIVE.add(player.getUUID());
                WINDUP_GROUNDED.remove(player.getUUID());
            } else {
                WINDUP_ACTIVE.remove(player.getUUID());
                WINDUP_GROUNDED.remove(player.getUUID());
            }
        } catch (Throwable ignored) {
            // BC optional.
        }
    }

    /** Receives a committed falling slash from the client. */
    public static void beginSlam(ServerPlayer player, float fallBlocks) {
        float fall = Math.max(fallBlocks, player.fallDistance);
        if (fall <= 5.0F) {
            return;
        }
        PENDING.put(player.getUUID(), fall);
        SLAM_ACTIVE.add(player.getUUID());
    }

    /**
     * Falling-slash landing rules: below 10 blocks the player is fall-damage immune; above 10 blocks
     * they lose 1 HP per extra 3 blocks, never below 1 HP. The real landing fall height is kept so the
     * hit damage can use it.
     */
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Float pending = PENDING.get(player.getUUID());
        if (pending == null || !SLAM_ACTIVE.remove(player.getUUID())) {
            return;
        }
        // The event carries the real landing fall height; the commit-time estimate is a few blocks short.
        float fall = Math.max(event.getDistance(), pending);
        LANDED_FALL.put(player.getUUID(), fall);
        event.setCanceled(true);
        if (fall <= 10.0F) {
            return;
        }
        int steps = (int) ((fall - 10.0F) / 3.0F);
        if (steps <= 0) {
            return;
        }
        float health = player.getHealth();
        player.setHealth(Math.max(1.0F, health - steps * 1.0F));
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Kept for non-slam fall hits; the independent slam settlement uses onPlayerTick instead.
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof Player) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player) || player.fallDistance <= 5.0F) {
            return;
        }
        FALL_IMPACT.put(player.getUUID(), player.fallDistance);
    }

    /**
     * Restores Better Combat's attack channel if the client never sent the matching restore packet
     * (for example after a client crash). Only counts ground time, so a long legitimate fall is
     * never cut short.
     */
    private static void tickWindupFailsafe(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!WINDUP_ACTIVE.contains(id)) {
            return;
        }
        if (!player.onGround()) {
            WINDUP_GROUNDED.remove(id);
            return;
        }
        int grounded = WINDUP_GROUNDED.merge(id, 1, Integer::sum);
        if (grounded > MAX_GROUNDED_WINDUP_TICKS) {
            setWindup(player, false);
        }
    }

    /** Applies the falling-slash damage and landing particles once the player is back on the ground. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        tickWindupFailsafe(serverPlayer);
        if (!player.onGround()) {
            return;
        }
        Float fall = PENDING.remove(player.getUUID());
        SLAM_ACTIVE.remove(player.getUUID());
        if (fall == null || !(player.level() instanceof ServerLevel level)) {
            LANDED_FALL.remove(player.getUUID());
            return;
        }
        Float landedFall = LANDED_FALL.remove(player.getUUID());
        float height = landedFall != null ? Math.max(landedFall, fall) : fall;

        // Landing sound supplied in resourcepacks/sound/landed.mp3.
        playSound(level, player, ModSounds.LANDED.get(), 1.0F, 1.0F);

        double baseDamage = 1.0D;
        var attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            baseDamage = attribute.getValue();
        }
        // Extra attack power grows with the fall height and has no cap:
        // blocks 1-10: +4 damage each, blocks 11-20: +2 each, every block beyond: +1 each.
        float damage = (float) (baseDamage + fallBonusDamage(height));
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() > 1.0E-6D) {
            flatLook = flatLook.normalize();
        }
        AABB area = player.getBoundingBox().inflate(4.0D, 2.5D, 4.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())) {
            Vec3 to = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
            if (to.lengthSqr() < 1.0E-6D) {
                continue;
            }
            double dot = to.normalize().dot(flatLook);
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))));
            if (angle > 105.0D || to.lengthSqr() > 16.0D) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.push(to.x * 0.35D, 0.25D, to.z * 0.35D);
        }

        ItemStack weapon = player.getMainHandItem();
        if (!Forge.particlesEnabled(weapon)) {
            weapon = player.getOffhandItem();
        }
        if (Forge.particlesEnabled(weapon)) {
            int color = Forge.displayColor(weapon);
            boolean glow = Forge.glowEnabled(weapon);
            for (int i = 0; i < 80; i++) {
                double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
                double radius = player.getRandom().nextDouble() * 1.8D;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                level.sendParticles(new BlockSparkOptions(color, glow),
                        x, player.getY() + 0.15D, z, 1, 0.2D, 0.25D, 0.2D, 0.15D);
            }
        }
    }

    /**
     * {@return the extra attack power granted by the fall height, with no upper limit}.
     *
     * <ul>
     *   <li>first 10 blocks: 4 damage per block</li>
     *   <li>blocks 10-20: 2 damage per block</li>
     *   <li>every block beyond 20: 1 damage per block</li>
     * </ul>
     */
    private static double fallBonusDamage(float fallHeight) {
        double height = Math.max(0.0D, fallHeight);
        double first = Math.min(height, 10.0D) * 4.0D;
        double second = Math.max(0.0D, Math.min(height, 20.0D) - 10.0D) * 2.0D;
        double rest = Math.max(0.0D, height - 20.0D);
        return first + second + rest;
    }

    /**
     * Applies the falling-slash damage to the creature hit while gliding with an elytra. The extra
     * attack power is taken from the impact speed: it is converted to the free-fall height that
     * reaches the same speed, so a glide at a given speed deals the same extra damage as a falling
     * slash hitting at that speed.
     */
    public static void settleGlideStrike(ServerPlayer player, int targetId, float speed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || target == player || target instanceof Player
                || !target.isAlive()) {
            return;
        }
        // Sanity: the collision happened next to the player; reject spoofed far-away targets.
        if (player.distanceToSqr(target) > 64.0D) {
            return;
        }
        double baseDamage = 1.0D;
        var attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            baseDamage = attribute.getValue();
        }
        float damage = (float) (baseDamage + fallBonusDamage(glideSpeedToFallHeight(speed)));
        target.hurt(player.damageSources().playerAttack(player), damage);
        Vec3 dir = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
        if (dir.lengthSqr() > 1.0E-6D) {
            dir = dir.normalize();
            target.push(dir.x * 0.45D, 0.25D, dir.z * 0.45D);
        }
    }

    /**
     * Converts an elytra impact speed (blocks per tick) into the free-fall height that reaches the
     * same speed: {@code v = sqrt(2 * g * h)} with Minecraft gravity {@code g = 0.08} blocks/tick²,
     * hence {@code h = v² / 0.16}. This keeps the glide slash extra damage balanced with the normal
     * falling slash, which scales with fall height.
     */
    private static float glideSpeedToFallHeight(float speed) {
        double v = Math.max(0.0D, speed);
        return (float) (v * v / (2.0D * 0.08D));
    }

    /**
     * Plays the claymore's third attack swing sound when the slam resumes from its frozen 1/3 pose at
     * four blocks above the ground, so the sound matches the actual chopping animation.
     */
    public static void playStrikeSound(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            ItemStack claymore = new ItemStack(ModItems.get("claymore").get());
            ResourceLocation soundId = thirdAttackSoundId(claymore);
            if (soundId == null) {
                return;
            }
            var sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (sound == null) {
                return;
            }
            playSound(level, player, sound, 1.0F, 1.0F);
        } catch (Throwable ignored) {
            // Better Combat optional.
        }
    }

    /**
     * Resolves the claymore's third attack swing sound from Better Combat's weapon attributes; falls
     * back to {@code bettercombat:claymore_slam} (the slam attack's sound in the data file).
     */
    private static ResourceLocation thirdAttackSoundId(ItemStack claymore) {
        try {
            Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
            Object attributes = registry.getMethod("getAttributes", ItemStack.class).invoke(null, claymore);
            if (attributes != null) {
                Object[] attacks = (Object[]) attributes.getClass().getMethod("attacks").invoke(attributes);
                if (attacks != null && attacks.length > 0) {
                    Object attack = attacks[Math.min(2, attacks.length - 1)];
                    Object swingSound = attack.getClass().getMethod("swingSound").invoke(attack);
                    if (swingSound == null) {
                        return ResourceLocation.fromNamespaceAndPath("bettercombat", "claymore_slam");
                    }
                    String id = (String) swingSound.getClass().getMethod("id").invoke(swingSound);
                    return ResourceLocation.tryParse(id);
                }
            }
        } catch (Throwable ignored) {
            // Fall back to the known sound id below.
        }
        return ResourceLocation.fromNamespaceAndPath("bettercombat", "claymore_slam");
    }

    private static void playSound(ServerLevel level, Player player, SoundEvent sound,
                                  float volume, float pitch) {
        if (sound == null) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    /** Compatibility entry kept for the BC attack-request mixin. */
    public static void markSlam(ServerPlayer player) {
        if (player.fallDistance > 5.0F) {
            SLAM_ACTIVE.add(player.getUUID());
        }
    }
}
