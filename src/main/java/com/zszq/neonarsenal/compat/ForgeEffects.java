package com.zszq.neonarsenal.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;
import com.zszq.neonarsenal.item.WeaponCooldowns;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Forge-layer combat effects. All cooldowns are stored on the weapon stack with absolute game-time
 * end ticks ({@link WeaponCooldowns}), so dropping/switching the weapon never resets them.
 *
 * <ul>
 *   <li><b>Fire charge</b>: hits ignite the target for 8 s (no enchantment glint).</li>
 *   <li><b>Chorus fruit</b>: pull hostiles (20 blocks, line of sight) + 4 s stun; 5 s weapon cooldown.</li>
 *   <li><b>Creeper head</b>: spawns a creeper that walks away from the player for 4 s while pulling
 *       living mobs within 6 blocks along its path, then swells/flashes white for 1.4 s and explodes
 *       (no terrain damage, hostiles only, half TNT damage); 8 s weapon cooldown.</li>
 *   <li><b>Rabbit foot</b>: while held +30% attack speed and +20% movement speed; hits summon four
 *       2-block-tall invulnerable rabbits that attack one hostile every 2 s for 8 damage and vanish
 *       after 10 s; 30 s weapon cooldown.</li>
 * </ul>
 */
public final class ForgeEffects {

    private static final int CHORUS_COOLDOWN_TICKS = 200;
    private static final int STUN_TICKS = 80;
    private static final double TELEPORT_RANGE = 20.0D;
    private static final double DESTINATION_RADIUS = 1.6D;

    private static final int CREEPER_COOLDOWN_TICKS = 600;
    private static final int CREEPER_WALK_TICKS = 80;
    private static final int CREEPER_FLASH_TICKS = 28;
    private static final double CREEPER_ATTRACT_RANGE = 6.0D;

    private static final int ECHO_COOLDOWN_TICKS = 600;
    private static final int ECHO_FRENZY_TICKS = 160;
    private static final double ECHO_RANGE = 8.0D;

    private static final int RABBIT_COOLDOWN_TICKS = 600;
    private static final int RABBIT_LIFETIME_TICKS = 200;
    private static final int RABBIT_ATTACK_INTERVAL = 10;
    private static final int RABBIT_MAX_TARGETS = 3;
    private static final float RABBIT_DAMAGE = 4.0F;
    private static final double RABBIT_SCALE = 4.0D;
    private static final double RABBIT_SEARCH_RANGE = 24.0D;
    private static final double RABBIT_REACH = 2.5D;

    private static final String CD_CHORUS = "chorus";
    private static final String CD_CREEPER = "creeper";
    private static final String CD_RABBIT = "rabbit";
    private static final String CD_ECHO = "echo";

    private static final ResourceLocation ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "forge_attack_speed");
    private static final ResourceLocation MOVE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "forge_move_speed");
    private static final ResourceLocation DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "forge_damage");

    private static final Map<LivingEntity, Long> STUNNED_UNTIL = new WeakHashMap<>();
    private static final Map<Rabbit, RabbitMinion> RABBIT_MINIONS = new WeakHashMap<>();
    private static final Map<Creeper, CreeperState> CREEPERS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> FRENZY_UNTIL = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> CREEPER_ATTRACTED = new WeakHashMap<>();

    private static final class RabbitMinion {
        private final long expire;
        private long nextAttack;

        private RabbitMinion(long expire, long nextAttack) {
            this.expire = expire;
            this.nextAttack = nextAttack;
        }
    }

    private static final class CreeperState {
        private final long walkEnd;
        private final long explodeAt;
        private boolean flashing;

        private CreeperState(long walkEnd) {
            this.walkEnd = walkEnd;
            this.explodeAt = walkEnd + CREEPER_FLASH_TICKS;
        }
    }

    private ForgeEffects() {
    }

    // ------------------------------------------------------------------ hit effects

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        if (AttackHandTracker.isOffHandAttack(player)) {
            // Dual wielding: off-hand attacks never trigger the main-hand weapon's forge effects.
            return;
        }
        if (Forge.hasRabbit(weapon) && JumpAttackHandler.isRecentJumpSlam(player)) {
            // Rabbit layer: jump-slam damage +100%.
            event.setAmount(event.getAmount() * 2.0F);
        }
        if (Forge.hasCreeper(weapon)) {
            try {
                triggerCreeper(player, weapon, event.getEntity());
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Creeper forge effect failed", throwable);
            }
        }
        if (Forge.hasChorus(weapon)) {
            try {
                triggerChorus(player, weapon);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Chorus forge effect failed", throwable);
            }
        }
        if (Forge.hasRabbit(weapon)) {
            try {
                triggerRabbit(player, weapon);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Rabbit forge effect failed", throwable);
            }
        }
        if (Forge.hasEcho(weapon)) {
            try {
                triggerEcho(player, weapon, event.getEntity());
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Echo forge effect failed", throwable);
            }
        }
    }

    private static void triggerChorus(Player player, ItemStack weapon) {
        long now = player.level().getGameTime();
        if (!WeaponCooldowns.ready(weapon, CD_CHORUS, now)) {
            return;
        }
        WeaponCooldowns.start(weapon, CD_CHORUS, now + CHORUS_COOLDOWN_TICKS);
        ServerLevel level = (ServerLevel) player.level();
        var mobs = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(TELEPORT_RANGE),
                entity -> entity != player && entity.isAlive() && isHostile(entity) && !isBoss(entity)
                        && player.hasLineOfSight(entity) && !isCreeperAttracted(entity));
        for (LivingEntity mob : mobs) {
            Vec3 direction = new Vec3(mob.getX() - player.getX(), 0.0D, mob.getZ() - player.getZ());
            if (direction.lengthSqr() < 1.0E-4D) {
                continue;
            }
            direction = direction.normalize().scale(DESTINATION_RADIUS);
            double oldX = mob.getX();
            double oldY = mob.getY() + mob.getBbHeight() * 0.5D;
            double oldZ = mob.getZ();
            mob.teleportTo(level, player.getX() + direction.x, player.getY(),
                    player.getZ() + direction.z, java.util.Set.of(), mob.getYRot(), mob.getXRot());
            double newX = mob.getX();
            double newY = mob.getY() + mob.getBbHeight() * 0.5D;
            double newZ = mob.getZ();
            level.sendParticles(ParticleTypes.PORTAL, oldX, oldY, oldZ, 32, 0.5D, 0.5D, 0.5D, 0.0D);
            level.sendParticles(ParticleTypes.PORTAL, newX, newY, newZ, 32, 0.5D, 0.5D, 0.5D, 0.0D);
            level.playSound(null, oldX, oldY, oldZ, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            level.playSound(null, newX, newY, newZ, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            STUNNED_UNTIL.put(mob, now + STUN_TICKS);
            if (mob instanceof Mob hostile) {
                hostile.setTarget(null);
                hostile.getNavigation().stop();
            }
        }
    }

    private static void triggerCreeper(Player player, ItemStack weapon, LivingEntity target) {
        long now = player.level().getGameTime();
        if (!WeaponCooldowns.ready(weapon, CD_CREEPER, now)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WeaponCooldowns.start(weapon, CD_CREEPER, now + CREEPER_COOLDOWN_TICKS);
        Vec3 spawn = target != null ? target.position() : player.position().add(player.getLookAngle().scale(2.0D));
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper == null) {
            return;
        }
        creeper.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180.0F, 0.0F);
        creeper.setPersistenceRequired();
        creeper.setInvulnerable(true);
        creeper.setSwellDir(0);
        creeper.addTag("bettercombat_neonarsenal_visual_creeper");
        level.addFreshEntity(creeper);
        CREEPERS.put(creeper, new CreeperState(now + CREEPER_WALK_TICKS));
    }

    /** Frenzies nearby hostile mobs so they attack each other; Deep Dark particles mark them. */
    private static void triggerEcho(Player player, ItemStack weapon, LivingEntity target) {
        if (target == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (!WeaponCooldowns.ready(weapon, CD_ECHO, now)) {
            return;
        }
        java.util.List<LivingEntity> mobs = new java.util.ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class, target.getBoundingBox().inflate(ECHO_RANGE),
                entity -> entity.isAlive() && isHostile(entity) && !isBoss(entity)
                        && !(entity instanceof Rabbit) && !isForgeCreeper(entity)));
        if (mobs.size() < 2) {
            return;
        }
        WeaponCooldowns.start(weapon, CD_ECHO, now + ECHO_COOLDOWN_TICKS);
        for (LivingEntity mob : mobs) {
            FRENZY_UNTIL.put(mob, now + ECHO_FRENZY_TICKS);
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    20, 0.4D, 0.6D, 0.4D, 0.02D);
        }
    }

    /** Makes frenzied mobs target and fight each other. */
    private static void tickFrenzy(ServerLevel level, Mob mob, long now) {
        LivingEntity other = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(24.0D),
                entity -> entity.isAlive() && entity != mob && isHostile(entity) && !isBoss(entity)
                        && !(entity instanceof Rabbit) && !isForgeCreeper(entity))) {
            boolean candidateFrenzied = FRENZY_UNTIL.containsKey(candidate);
            double distance = mob.distanceToSqr(candidate);
            if (candidateFrenzied) {
                distance *= 0.5D;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                other = candidate;
            }
        }
        if (other != null) {
            mob.setTarget(other);
            mob.getNavigation().moveTo(other, 1.1D);
            if (now % 10L == 0L) {
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                        4, 0.3D, 0.4D, 0.3D, 0.01D);
            }
        }
    }

    private static void triggerRabbit(Player player, ItemStack weapon) {
        long now = player.level().getGameTime();
        if (!WeaponCooldowns.ready(weapon, CD_RABBIT, now)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WeaponCooldowns.start(weapon, CD_RABBIT, now + RABBIT_COOLDOWN_TICKS);
        for (int i = 0; i < 4; i++) {
            Rabbit rabbit = EntityType.RABBIT.create(level);
            if (rabbit == null) {
                continue;
            }
            double angle = (Math.PI / 2.0D) * i + level.getRandom().nextDouble() * 0.4D;
            double x = player.getX() + Math.cos(angle) * 2.0D;
            double z = player.getZ() + Math.sin(angle) * 2.0D;
            rabbit.moveTo(x, player.getY(), z, 0.0F, 0.0F);
            rabbit.setPersistenceRequired();
            rabbit.setInvulnerable(true);
            rabbit.addTag("bettercombat_neonarsenal_summoned_rabbit");
            rabbit.getPersistentData().putLong("bettercombat_neonarsenal_expire", now + RABBIT_LIFETIME_TICKS);
            AttributeInstance scale = rabbit.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(RABBIT_SCALE);
            }
            AttributeInstance speed = rabbit.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * 2.0D);
            }
            level.addFreshEntity(rabbit);
            RABBIT_MINIONS.put(rabbit, new RabbitMinion(now + RABBIT_LIFETIME_TICKS, now + RABBIT_ATTACK_INTERVAL));
        }
    }

    // ------------------------------------------------------------------ held bonuses

    /** Applies the held bonuses of every forge layer on the main-hand weapon. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        double attackSpeed = 0.0D;
        double damage = 0.0D;
        double moveSpeed = 0.0D;
        if (weapon.getItem() instanceof SharpnessWeaponItem) {
            for (String layer : Forge.layers(weapon)) {
                switch (layer) {
                    case Forge.CHORUS -> {
                        damage += 0.10D;
                        moveSpeed += 0.15D;
                    }
                    case Forge.CREEPER -> {
                        damage += 0.05D;
                        attackSpeed += 0.15D;
                    }
                    case Forge.RABBIT -> attackSpeed += 0.05D;
                    case Forge.ECHO -> {
                        attackSpeed -= 0.50D;
                        damage += 1.00D;
                    }
                    default -> {
                        // No held bonus.
                    }
                }
            }
        }
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, attackSpeed);
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, damage);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_SPEED_ID, moveSpeed);
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) {
            return;
        }
        if (amount == 0.0D) {
            attribute.removeModifier(id);
        } else {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Rabbit layer: +150% jump height (2.5x default height; velocity scales with sqrt(2.5)). */
    public static void onLivingJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SharpnessWeaponItem) || !Forge.hasRabbit(weapon)) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        double boosted = 0.42D * Math.sqrt(2.5D);
        if (movement.y < boosted) {
            player.setDeltaMovement(movement.x, boosted, movement.z);
        }
    }

    // ------------------------------------------------------------------ ticking

    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        long now = entity.level().getGameTime();
        if (entity instanceof Rabbit rabbit
                && (RABBIT_MINIONS.containsKey(rabbit)
                || rabbit.getPersistentData().contains("bettercombat_neonarsenal_expire"))) {
            // Expiry is stored on the entity itself so a lost in-memory map can never prevent it.
            long expire = rabbit.getPersistentData().getLong("bettercombat_neonarsenal_expire");
            if (expire <= 0L) {
                expire = now + RABBIT_LIFETIME_TICKS;
                rabbit.getPersistentData().putLong("bettercombat_neonarsenal_expire", expire);
            }
            if (!rabbit.isAlive()) {
                RABBIT_MINIONS.remove(rabbit);
            } else if (now >= expire) {
                RABBIT_MINIONS.remove(rabbit);
                if (rabbit.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CLOUD,
                            rabbit.getX(), rabbit.getY() + rabbit.getBbHeight() * 0.5D, rabbit.getZ(),
                            24, 0.4D, 0.5D, 0.4D, 0.02D);
                }
                rabbit.discard();
                return;
            } else {
                // Completely invulnerable: keep it healthy no matter what hits it.
                rabbit.setInvulnerable(true);
                rabbit.setHealth(rabbit.getMaxHealth());
                long expireAt = expire;
                RabbitMinion minion = RABBIT_MINIONS.computeIfAbsent(rabbit,
                        ignored -> new RabbitMinion(expireAt, now + RABBIT_ATTACK_INTERVAL));
                // The rabbits never attack each other; they actively hunt hostile mobs.
                seekHostile(rabbit);
                if (now >= minion.nextAttack) {
                    minion.nextAttack = now + RABBIT_ATTACK_INTERVAL;
                    attackNearby(rabbit);
                }
            }
        }
        if (entity instanceof Creeper creeper) {
            CreeperState state = CREEPERS.get(creeper);
            if (state != null && !(creeper.level() instanceof ServerLevel level)) {
                CREEPERS.remove(creeper);
            } else if (state != null) {
                tickCreeper((ServerLevel) creeper.level(), creeper, state, now);
            }
        }
        Long frenzyUntil = FRENZY_UNTIL.get(entity);
        if (frenzyUntil != null) {
            if (now >= frenzyUntil || !(entity.level() instanceof ServerLevel level)) {
                FRENZY_UNTIL.remove(entity);
            } else if (entity instanceof Mob mob) {
                tickFrenzy(level, mob, now);
            }
        }
        if (STUNNED_UNTIL.isEmpty()) {
            return;
        }
        Long until = STUNNED_UNTIL.get(entity);
        if (until == null) {
            return;
        }
        if (now >= until) {
            STUNNED_UNTIL.remove(entity);
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    /** Mobs being pulled by a creeper-head creeper are immune to chorus teleports until it explodes. */
    private static boolean isCreeperAttracted(LivingEntity entity) {
        Long until = CREEPER_ATTRACTED.get(entity);
        return until != null && entity.level().getGameTime() < until;
    }

    private static boolean isForgeCreeper(LivingEntity entity) {
        return entity instanceof Creeper creeper
                && (CREEPERS.containsKey(creeper) || creeper.getTags().contains("bettercombat_neonarsenal_visual_creeper"));
    }

    private static void tickCreeper(ServerLevel level, Creeper creeper, CreeperState state, long now) {
        // The walking/flashing creeper is completely invulnerable until it explodes on its own.
        creeper.setInvulnerable(true);
        creeper.setHealth(creeper.getMaxHealth());
        if (now >= state.explodeAt) {
            NeonArsenal.LOGGER.info("Creeper forge: swell finished, custom explosion (no terrain damage)");
            CREEPERS.remove(creeper);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    creeper.getX(), creeper.getY() + 0.6D, creeper.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            creeper.discard();
            CREEPER_ATTRACTED.clear();
            explodeCreeper(level, creeper);
            return;
        }
        // Attract living mobs within 6 blocks along the walking path.
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                creeper.getBoundingBox().inflate(CREEPER_ATTRACT_RANGE),
                entity -> entity.isAlive() && entity != creeper && !(entity instanceof Player)
                        && entity != creeper)) {
            Vec3 direction = new Vec3(creeper.getX() - mob.getX(), 0.0D, creeper.getZ() - mob.getZ());
            double distance = Math.sqrt(direction.lengthSqr());
            if (distance < 1.2D || distance < 1.0E-4D) {
                continue;
            }
            double step = Math.min(0.12D, distance - 1.1D);
            direction = direction.normalize().scale(step);
            CREEPER_ATTRACTED.put(mob, state.explodeAt);
            mob.teleportTo(level, mob.getX() + direction.x, mob.getY(), mob.getZ() + direction.z,
                    java.util.Set.of(), mob.getYRot(), mob.getXRot());
            if (mob instanceof Mob hostile) {
                hostile.setTarget(null);
                hostile.getNavigation().stop();
            }
        }
        if (now < state.walkEnd) {
            // Walk straight away from the player.
            Vec3 away = new Vec3(creeper.getX() - nearestPlayerX(creeper), 0.0D,
                    creeper.getZ() - nearestPlayerZ(creeper));
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(creeper.getZ(), 0.0D, -creeper.getX());
            }
            away = away.normalize();
            creeper.setSwellDir(0);
            creeper.setTarget(null);
            creeper.getNavigation().stop();
            creeper.setYRot((float) (Math.toDegrees(Math.atan2(away.x, away.z))));
            creeper.setYHeadRot(creeper.getYRot());
            // Fast, direct retreat: 0.25 blocks per tick (~5 blocks/s), independent of pathfinding.
            creeper.move(MoverType.SELF, new Vec3(away.x * 0.25D, -0.08D, away.z * 0.25D));
            creeper.setDeltaMovement(0.0D, creeper.getDeltaMovement().y, 0.0D);
        } else {
            if (!state.flashing) {
                state.flashing = true;
                NeonArsenal.LOGGER.info("Creeper forge: walked away for 4 s, now swelling/flashing for 1.4 s");
            }
            // Stop and start the vanilla creeper white-flash swell.
            creeper.getNavigation().stop();
            creeper.setTarget(null);
            creeper.setSwellDir(1);
        }
    }

    private static double nearestPlayerX(Creeper creeper) {
        Player nearest = creeper.level().getNearestPlayer(creeper, 64.0D);
        return nearest != null ? nearest.getX() : creeper.getX();
    }

    private static double nearestPlayerZ(Creeper creeper) {
        Player nearest = creeper.level().getNearestPlayer(creeper, 64.0D);
        return nearest != null ? nearest.getZ() : creeper.getZ();
    }

    private static void explodeCreeper(ServerLevel level, Creeper creeper) {
        double x = creeper.getX();
        double y = creeper.getY() + 0.5D;
        double z = creeper.getZ();
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(x - 4.0D, y - 4.0D, z - 4.0D,
                        x + 4.0D, y + 4.0D, z + 4.0D),
                entity -> entity.isAlive() && isHostile(entity) && !isBoss(entity))) {
            double distance = Math.sqrt(mob.distanceToSqr(x, y, z));
            float damage = (float) (20.0D * Math.max(0.25D, 1.0D - distance / 4.0D * 0.75D));
            mob.hurt(level.damageSources().explosion(null, null), damage);
            mob.knockback(1.5D, x - mob.getX(), z - mob.getZ());
        }
    }

    /** Bites up to three hostile mobs within reach; never hits rabbits. */
    private static void attackNearby(Rabbit rabbit) {
        if (!(rabbit.level() instanceof ServerLevel level)) {
            return;
        }
        java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                rabbit.getBoundingBox().inflate(RABBIT_REACH),
                entity -> entity.isAlive() && isHostile(entity) && !isBoss(entity)
                        && !(entity instanceof Rabbit) && !isForgeCreeper(entity));
        targets.sort(java.util.Comparator.comparingDouble(entity -> rabbit.distanceToSqr(entity)));
        int hit = 0;
        for (LivingEntity target : targets) {
            if (hit >= RABBIT_MAX_TARGETS) {
                break;
            }
            target.hurt(level.damageSources().mobAttack(rabbit), RABBIT_DAMAGE);
            hit++;
        }
        if (hit > 0) {
            rabbit.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Actively hunts the nearest hostile mob; rabbits are never valid targets. */
    private static LivingEntity seekHostile(Rabbit rabbit) {
        if (!(rabbit.level() instanceof ServerLevel level)) {
            return null;
        }
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                rabbit.getBoundingBox().inflate(RABBIT_SEARCH_RANGE),
                entity -> entity.isAlive() && isHostile(entity) && !isBoss(entity)
                        && !(entity instanceof Rabbit) && !isForgeCreeper(entity))) {
            double distance = rabbit.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null) {
            rabbit.getNavigation().moveTo(best, 1.6D);
        }
        return best;
    }

    /** Stunned mobs cannot deal damage; summoned rabbits cannot be damaged at all. */
    public static void onStunDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Rabbit rabbit && RABBIT_MINIONS.containsKey(rabbit)) {
            event.setAmount(0.0F);
            return;
        }
        if (event.getEntity() instanceof Creeper creeper && isForgeCreeper(creeper)) {
            event.setAmount(0.0F);
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isStunned(attacker)) {
            event.setAmount(0.0F);
        }
    }

    private static boolean isStunned(LivingEntity entity) {
        Long until = STUNNED_UNTIL.get(entity);
        return until != null && entity.level().getGameTime() < until;
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss
                || entity.getMaxHealth() >= 200.0F;
    }
}
