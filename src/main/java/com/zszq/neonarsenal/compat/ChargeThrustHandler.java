package com.zszq.neonarsenal.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.weapon.WeaponDefinition.Ability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of the mounted charge thrust (冲锋前刺).
 *
 * <p>While the client holds the charge (its windup packet keeps Better Combat's attack channel
 * disabled), the server sweeps the player's movement path every tick and damages every creature the
 * spear passes through. On release the remaining creatures in front are hit by the completed thrust.
 * Damage is charge damage only when the attacker and target relative velocity, projected onto the
 * attack direction, is at least {@link #MIN_CHARGE_SPEED} blocks per second; a head-on dash therefore
 * hits much harder than chasing a target running away, and a target moving sideways contributes
 * little. The multiplier grows linearly above the threshold and is capped.</p>
 *
 * <p>The attacker velocity comes from the server-side position history of the rider (not the mount's
 * delta movement, which a client-authoritative mount keeps at zero on the server). Elytra gliding
 * does not trigger the charge thrust: it requires a living mount.</p>
 */
public final class ChargeThrustHandler {

    /** Minimum relative velocity (blocks per second) along the attack direction for charge damage. */
    public static final double MIN_CHARGE_SPEED = 4.6D;
    /** Extra damage multiplier per block/second above the threshold. */
    private static final double DAMAGE_PER_SPEED = 0.15D;
    /** Cap for the charge damage multiplier. */
    private static final double MAX_MULTIPLIER = 4.0D;
    /** Horizontal reach of the thrust, in blocks (vanilla reach plus the spear's range bonus). */
    private static final double REACH = 4.0D;
    /** Half-angle of the forward thrust cone. */
    private static final double MIN_DOT = Math.cos(Math.toRadians(60.0D));
    /** Safety net: a charge never lasts longer than 10 s (the client normally stops it earlier). */
    private static final int MAX_CHARGE_TICKS = 20 * 10;

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    private static final class Charge {
        private final java.util.ArrayDeque<Vec3> positions = new java.util.ArrayDeque<>();
        private final Set<UUID> hit = new HashSet<>();
        private Vec3 lastRiderPosition;
        private long startTick;

        private void capture(Vec3 position) {
            positions.addLast(position);
            while (positions.size() > 6) {
                positions.removeFirst();
            }
        }

        /** {@return the average movement per tick over the tracked window}. */
        private Vec3 velocity() {
            if (positions.size() < 2) {
                return Vec3.ZERO;
            }
            Vec3 first = positions.peekFirst();
            Vec3 last = positions.peekLast();
            return last.subtract(first).scale(1.0D / (positions.size() - 1));
        }
    }

    private ChargeThrustHandler() {
    }

    /** Client windup toggle: starts or stops the server-side charge and Better Combat attack gating. */
    public static void setWindup(ServerPlayer player, boolean active) {
        if (!active) {
            endCharge(player);
            return;
        }
        if (!eligible(player)) {
            endCharge(player);
            return;
        }
        Charge charge = new Charge();
        charge.capture(motionAnchor(player));
        charge.lastRiderPosition = player.position();
        charge.startTick = player.level().getGameTime();
        CHARGES.put(player.getUUID(), charge);
        try {
            net.bettercombat.api.CombatFlags.setAttacksDisabled(player, true);
        } catch (Throwable ignored) {
            // Better Combat optional.
        }
    }

    /** Client released the charge: finish the thrust and apply the released strike. */
    public static void strike(ServerPlayer player, boolean charged) {
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) {
            return;
        }
        if (!eligible(player)) {
            endCharge(player);
            return;
        }
        Vec3 direction = attackDirection(player);
        charge.capture(motionAnchor(player));
        List<LivingEntity> targets = sweep(player, player.position(), player.position(), direction);
        applyHits(player, charge, targets, direction, charged);
        playStabSound(player);
        endCharge(player);
    }

    /** Sweeps the movement path every tick while the client keeps the charge active. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) {
            return;
        }
        if (player.isDeadOrDying() || !eligible(player)
                || player.level().getGameTime() - charge.startTick > MAX_CHARGE_TICKS) {
            endCharge(player);
            return;
        }
        Vec3 from = charge.lastRiderPosition != null ? charge.lastRiderPosition : player.position();
        Vec3 to = player.position();
        charge.lastRiderPosition = to;
        charge.capture(motionAnchor(player));
        Vec3 direction = attackDirection(player);
        List<LivingEntity> targets = sweep(player, from, to, direction);
        applyHits(player, charge, targets, direction, true);
    }

    /** Clears the charge state on disconnect so a rejoin starts clean. */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endCharge(player);
        }
    }

    private static void endCharge(ServerPlayer player) {
        CHARGES.remove(player.getUUID());
        try {
            net.bettercombat.api.CombatFlags.setAttacksDisabled(player, false);
        } catch (Throwable ignored) {
            // Better Combat optional.
        }
    }

    /** {@return whether the player still satisfies the charge conditions}. */
    private static boolean eligible(ServerPlayer player) {
        if (player.isSpectator() || player.isUsingItem() || player.getAbilities().flying) {
            return false;
        }
        if (!(player.getVehicle() instanceof LivingEntity)) {
            return false;
        }
        return JumpAttackHandler.hasAbility(player.getMainHandItem(), Ability.CHARGE_THRUST);
    }

    /**
     * {@return the position whose per-tick movement is used as the attacker's velocity}. Riding a
     * player-controlled mount is client-authoritative, so the mount position (synced from the vehicle
     * move packet) is the reliable motion source; otherwise the player's own position is used.
     */
    private static Vec3 motionAnchor(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null ? vehicle.position() : player.position();
    }

    /** {@return the normalized direction the spear is pointing (the player's look direction)}. */
    private static Vec3 attackDirection(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return look.normalize();
    }

    /** Collects living creatures inside the forward thrust cone that touches the swept path. */
    private static List<LivingEntity> sweep(ServerPlayer player, Vec3 from, Vec3 to, Vec3 direction) {
        List<LivingEntity> result = new ArrayList<>();
        Vec3 movement = to.subtract(from);
        AABB area = player.getBoundingBox()
                .expandTowards(movement)
                .expandTowards(direction.scale(REACH))
                .inflate(0.8D);
        Vec3 eye = player.getEyePosition();
        for (Entity entity : player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
                        && e != player.getVehicle()
                        && !(e instanceof Player) && !(e instanceof ArmorStand))) {
            LivingEntity target = (LivingEntity) entity;
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 toTarget = center.subtract(eye);
            double distance = toTarget.length();
            if (distance > REACH + target.getBbWidth() * 0.5D + 0.5D) {
                continue;
            }
            if (distance < 1.0E-4D || toTarget.normalize().dot(direction) >= MIN_DOT) {
                result.add(target);
            }
        }
        return result;
    }

    private static void applyHits(ServerPlayer player, Charge charge, List<LivingEntity> targets,
                                  Vec3 direction, boolean charged) {
        if (targets.isEmpty()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // The charge is a main-hand skill: drop any recorded Better Combat attack hand so the
        // forge/sharpness damage calculations use the main-hand weapon.
        AttackHandTracker.clear(player.getUUID());
        Vec3 attackerVelocity = charge.velocity();
        for (LivingEntity target : targets) {
            UUID id = target.getUUID();
            if (charge.hit.contains(id)) {
                continue;
            }
            double relativeSpeed = 0.0D;
            if (charged) {
                Vec3 relative = attackerVelocity.subtract(target.getDeltaMovement());
                relativeSpeed = relative.dot(direction) * 20.0D;
                // Only a real charge (head-on at speed) deals charge damage; low relative speed
                // passes through without damage, so a later faster pass can still hit.
                if (relativeSpeed < MIN_CHARGE_SPEED) {
                    continue;
                }
            }
            double multiplier = charged
                    ? Math.min(MAX_MULTIPLIER,
                            1.0D + (relativeSpeed - MIN_CHARGE_SPEED) * DAMAGE_PER_SPEED)
                    : 1.0D;
            float damage = (float) (baseDamage * multiplier);
            charge.hit.add(id);
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                target.push(direction.x * 0.4D, 0.2D, direction.z * 0.4D);
                spawnHitParticles(player, target, weapon);
            }
        }
    }

    private static void spawnHitParticles(ServerPlayer player, LivingEntity target, ItemStack weapon) {
        if (!(player.level() instanceof ServerLevel level) || !Forge.particlesEnabled(weapon)) {
            return;
        }
        int color = Forge.displayColor(weapon);
        boolean glow = Forge.glowEnabled(weapon);
        level.sendParticles(new BlockSparkOptions(color, glow),
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                8, 0.25D, 0.25D, 0.25D, 0.2D);
    }

    private static void playStabSound(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(
                    ResourceLocation.fromNamespaceAndPath("bettercombat", "spear_stab"));
            if (sound != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), sound,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } catch (Throwable ignored) {
            // Better Combat optional.
        }
    }
}
