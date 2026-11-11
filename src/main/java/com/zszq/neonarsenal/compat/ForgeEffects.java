package com.zszq.neonarsenal.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.item.ForgeStats;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;
import com.zszq.neonarsenal.item.WeaponCooldowns;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Forge-layer combat effects. All cooldowns are stored on the weapon stack with absolute game-time
 * end ticks ({@link WeaponCooldowns}), so dropping/switching the weapon never resets them.
 *
 * <p>Unlock rule: forging one or two copies of a material only grants that material's carried
 * ("while held") bonus; the material's on-hit effect unlocks at three copies and the cooldown is
 * halved at the full five copies (see {@link Forge#hitEffectActive}, {@link Forge#cooldownTicks}).
 * Attack speed and movement speed are held bonuses of the main-hand weapon (applied as player
 * attribute modifiers); every forged layer also grants +2% critical chance. The forge damage
 * percentage, the forged-material type multiplier, the critical chance and the sharpness multiplier
 * are applied at hit time with the weapon of the hand that actually attacked, so main- and off-hand
 * attacks are calculated separately. A jump slash always uses the main-hand weapon's bonuses.</p>
 *
 * <ul>
 *   <li><b>Chorus fruit</b>: pull hostiles (20 blocks, line of sight) + 4 s stun; 10 s weapon cooldown
 *       (5 s at five layers).</li>
 *   <li><b>Creeper head</b>: spawns a creeper that pathfinds away from the player for 4 s while
 *       pulling living mobs within 6 blocks, then swells/flashes white for 1.4 s and explodes
 *       (no terrain damage, hostiles only); 30 s weapon cooldown (15 s at five layers).</li>
 *   <li><b>Rabbit foot</b>: held jump height / jump-slam damage scale with its level; hits summon
 *       four 2-block-tall invulnerable rabbits that attack hostiles for 4 damage each; 30 s weapon
 *       cooldown (15 s at five layers).</li>
 *   <li><b>Echo shard</b>: fixed -20% attack speed, damage scales with level; hostiles fight each
 *       other for 8 s; 30 s weapon cooldown (15 s at five layers).</li>
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
    /** Detection/flee radius of the vanilla-style player avoidance goal (creeper vs cat behaviour). */
    private static final float CREEPER_FLEE_DISTANCE = 24.0F;
    private static final double CREEPER_ATTRACT_RANGE = 6.0D;
    private static final double CREEPER_ATTRACT_STEP = 0.45D;

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

    private static final String TAG_FORGE_CREEPER = "bettercombat_neonarsenal_visual_creeper";
    private static final String TAG_SUMMONED_RABBIT = "bettercombat_neonarsenal_summoned_rabbit";
    private static final String RABBIT_EXPIRE_KEY = "bettercombat_neonarsenal_expire";

    private static final String CD_CHORUS = "chorus";
    private static final String CD_CREEPER = "creeper";
    private static final String CD_RABBIT = "rabbit";
    private static final String CD_ECHO = "echo";

    private static final ResourceLocation ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "forge_attack_speed");
    private static final ResourceLocation MOVE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "forge_move_speed");

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
        private final Set<UUID> attracted = new HashSet<>();
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
        if (event.isCanceled()) {
            // Blocking cancels the damage after this listener runs in registration order; forge
            // on-hit effects must not fire on a completely blocked hit.
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        boolean offHand = AttackHandTracker.isOffHandAttack(player);
        boolean jumpSlam = JumpAttackHandler.isRecentJumpSlam(player);
        if (jumpSlam && offHand) {
            // Jump slashes always settle with the main-hand weapon. Better Combat only performs a
            // jump slash with the off hand when the main hand is not an arsenal weapon, so this
            // off-hand hit has no forge/sharpness bonus to apply.
            return;
        }
        // Dual wielding: each hand uses its own weapon's values. Better Combat swaps the hand stacks
        // while it performs an off-hand attack, so the off-hand slot holds the attacking weapon; on
        // every other hit the main hand does.
        ItemStack weapon = AttackHandTracker.attackingWeapon(player);
        if (!(weapon.getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        ForgeStats.Stats forge = ForgeStats.of(weapon);
        // The forge damage bonus is applied at hit time instead of as a permanent player
        // ATTACK_DAMAGE modifier, so main- and off-hand hits each use their own weapon's value.
        if (forge.damage() != 0.0D) {
            event.setAmount(event.getAmount() * (float) (1.0D + forge.damage()));
        }
        // Mixing different forge materials multiplies the total damage of the hit (x1 ... x1.5, see
        // ForgeStats#typeDamageMultiplier). All other damage steps are multiplicative too, so the
        // order does not matter; applying it here covers normal, jump-slam and skill damage alike.
        double typeDamage = ForgeStats.typeDamageMultiplier(weapon);
        if (typeDamage != 1.0D) {
            event.setAmount(event.getAmount() * (float) typeDamage);
        }
        if (jumpSlam && Forge.hasRabbit(weapon)) {
            // Rabbit layer: jump-slam damage scales with the rabbit level (+20% ... +100%).
            event.setAmount(event.getAmount() * (float) (1.0D + forge.jumpSlam()));
        }
        if (!offHand) {
            // Off-hand hits keep the original rule: only main-hand hits trigger the forge effects
            // (and consume their cooldowns).
            triggerOnHitEffects(player, weapon, event.getEntity());
        }
        applyCrit(player, weapon, event);
    }

    /** Runs the on-hit forge effects of the main-hand weapon (unlocked at three copies each). */
    private static void triggerOnHitEffects(Player player, ItemStack weapon, LivingEntity target) {
        // A material's on-hit effect unlocks once that same material has been forged three times.
        if (Forge.hitEffectActive(weapon, Forge.CREEPER)) {
            try {
                triggerCreeper(player, weapon, target);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Creeper forge effect failed", throwable);
            }
        }
        if (Forge.hitEffectActive(weapon, Forge.CHORUS)) {
            try {
                triggerChorus(player, weapon);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Chorus forge effect failed", throwable);
            }
        }
        if (Forge.hitEffectActive(weapon, Forge.RABBIT)) {
            try {
                triggerRabbit(player, weapon);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Rabbit forge effect failed", throwable);
            }
        }
        if (Forge.hitEffectActive(weapon, Forge.ECHO)) {
            try {
                triggerEcho(player, weapon, target);
            } catch (Throwable throwable) {
                NeonArsenal.LOGGER.warn("Echo forge effect failed", throwable);
            }
        }
    }

    /** Forge critical hit: +2% chance per layer, 1.5x damage with the vanilla crit particles/sound. */
    private static void applyCrit(Player player, ItemStack weapon,
                                  net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        double chance = ForgeStats.critChance(weapon);
        if (chance <= 0.0D || player.getRandom().nextDouble() >= chance) {
            return;
        }
        event.setAmount(event.getAmount() * 1.5F);
        if (player.level() instanceof ServerLevel level) {
            LivingEntity target = event.getEntity();
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.4D, 0.4D, 0.4D, 0.2D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void triggerChorus(Player player, ItemStack weapon) {
        long now = player.level().getGameTime();
        if (!WeaponCooldowns.ready(weapon, CD_CHORUS, now)) {
            return;
        }
        WeaponCooldowns.start(weapon, CD_CHORUS,
                now + Forge.cooldownTicks(weapon, Forge.CHORUS, CHORUS_COOLDOWN_TICKS));
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
        WeaponCooldowns.start(weapon, CD_CREEPER,
                now + Forge.cooldownTicks(weapon, Forge.CREEPER, CREEPER_COOLDOWN_TICKS));
        Vec3 spawn = target != null ? target.position() : player.position().add(player.getLookAngle().scale(2.0D));
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper == null) {
            return;
        }
        creeper.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180.0F, 0.0F);
        creeper.setPersistenceRequired();
        creeper.setInvulnerable(true);
        creeper.setSwellDir(0);
        creeper.setTarget(null);
        creeper.addTag(TAG_FORGE_CREEPER);
        // No player targeting while the creeper runs away (otherwise SwellGoal/MeleeAttackGoal would
        // fight the avoidance goal).
        creeper.targetSelector.removeAllGoals(goal -> true);
        // Same goal vanilla creepers use to flee cats: real pathfinding and sprinting away. The
        // radius is widened to cover the effect's 4 s retreat so the creeper genuinely leaves.
        creeper.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(
                creeper, Player.class, CREEPER_FLEE_DISTANCE, 1.0D, 1.2D));
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
        WeaponCooldowns.start(weapon, CD_ECHO,
                now + Forge.cooldownTicks(weapon, Forge.ECHO, ECHO_COOLDOWN_TICKS));
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
        WeaponCooldowns.start(weapon, CD_RABBIT,
                now + Forge.cooldownTicks(weapon, Forge.RABBIT, RABBIT_COOLDOWN_TICKS));
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
            rabbit.addTag(TAG_SUMMONED_RABBIT);
            rabbit.getPersistentData().putLong(RABBIT_EXPIRE_KEY, now + RABBIT_LIFETIME_TICKS);
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

    /** Last layer string whose held bonuses were applied, per player (avoids per-tick churn). */
    private static final Map<UUID, String> LAST_LAYER_KEY = new HashMap<>();

    /**
     * Applies the held main-hand bonuses (attack speed, movement speed; only on change). The forge
     * damage percentage is intentionally not a player attribute: it is applied per hit in
     * {@link #onIncomingDamage} so main- and off-hand attacks each use their own weapon's value.
     */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        ItemStack weapon = player.getMainHandItem();
        String key = weapon.getItem() instanceof SharpnessWeaponItem ? Forge.rawLayers(weapon) : "";
        String last = LAST_LAYER_KEY.get(id);
        if (last != null && last.equals(key)) {
            return;
        }
        LAST_LAYER_KEY.put(id, key);
        ForgeStats.Stats stats = ForgeStats.of(weapon);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, stats.attackSpeed());
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_SPEED_ID, stats.moveSpeed());
    }

    /** Forgets the cached bonus state of a player that left the server. */
    public static void clear(UUID id) {
        LAST_LAYER_KEY.remove(id);
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

    /** Rabbit layer: jump height scales with its level (+30% ... +150%). */
    public static void onLivingJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SharpnessWeaponItem) || !Forge.hasRabbit(weapon)) {
            return;
        }
        double jumpHeight = ForgeStats.of(weapon).jumpHeight();
        if (jumpHeight <= 0.0D) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        double boosted = 0.42D * Math.sqrt(1.0D + jumpHeight);
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
        boolean maybeForgeEntity = entity instanceof Rabbit || entity instanceof Creeper;
        if (!maybeForgeEntity && RABBIT_MINIONS.isEmpty() && CREEPERS.isEmpty()
                && FRENZY_UNTIL.isEmpty() && STUNNED_UNTIL.isEmpty() && CREEPER_ATTRACTED.isEmpty()) {
            // Nothing forge-related is active: skip the map lookups for every ordinary mob.
            return;
        }
        if (isForgeSummon(entity)) {
            // Summons ignore all external damage and knockback; the knockback-resistance attribute
            // also stops vanilla explosion pushes that bypass a damage event.
            entity.setInvulnerable(true);
            AttributeInstance knockbackResistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockbackResistance != null) {
                knockbackResistance.setBaseValue(1.0D);
            }
        }
        if (entity instanceof Rabbit rabbit
                && (RABBIT_MINIONS.containsKey(rabbit)
                || rabbit.getPersistentData().contains(RABBIT_EXPIRE_KEY))) {
            // Expiry is stored on the entity itself so a lost in-memory map can never prevent it.
            long expire = rabbit.getPersistentData().getLong(RABBIT_EXPIRE_KEY);
            if (expire <= 0L) {
                expire = now + RABBIT_LIFETIME_TICKS;
                rabbit.getPersistentData().putLong(RABBIT_EXPIRE_KEY, expire);
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
                && (CREEPERS.containsKey(creeper) || creeper.getTags().contains(TAG_FORGE_CREEPER));
    }

    private static void tickCreeper(ServerLevel level, Creeper creeper, CreeperState state, long now) {
        // The walking/flashing creeper is completely invulnerable until it explodes on its own.
        creeper.setInvulnerable(true);
        creeper.setHealth(creeper.getMaxHealth());
        if (now >= state.explodeAt) {
            NeonArsenal.LOGGER.debug("Creeper forge: swell finished, custom explosion (no terrain damage)");
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
        // Strong, locked attraction: mobs acquired once stay locked to this creeper until it
        // explodes, even if they were pushed out of the 6-block acquisition radius.
        java.util.Iterator<UUID> locked = state.attracted.iterator();
        while (locked.hasNext()) {
            UUID id = locked.next();
            Entity entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity mob) || !mob.isAlive()
                    || mob instanceof Player || isDummy(mob) || isForgeSummon(mob)) {
                locked.remove();
                continue;
            }
            attract(level, creeper, mob, state);
        }
        // Acquire hostile mobs within 6 blocks; only hostiles (dummies and summons excluded).
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                creeper.getBoundingBox().inflate(CREEPER_ATTRACT_RANGE),
                entity -> entity.isAlive() && entity != creeper && !(entity instanceof Player)
                        && isHostile(entity) && !isBoss(entity)
                        && !isForgeSummon(entity) && !isDummy(entity))) {
            if (state.attracted.add(mob.getUUID())) {
                attract(level, creeper, mob, state);
            }
        }
        if (now < state.walkEnd) {
            // Flee via the vanilla AvoidEntityGoal (same AI as a creeper avoiding a cat): the goal
            // owns the navigation and pathfinds around terrain, so no direct position movement here.
            creeper.setSwellDir(0);
            creeper.setTarget(null);
        } else {
            if (!state.flashing) {
                state.flashing = true;
                // Stop fleeing and stand still to play the vanilla white-flash swell: keep only the
                // float/swell goals so no navigation goal walks the creeper away from its spot.
                creeper.goalSelector.removeAllGoals(goal ->
                        !(goal instanceof net.minecraft.world.entity.ai.goal.FloatGoal)
                                && !(goal instanceof net.minecraft.world.entity.ai.goal.SwellGoal));
                creeper.getNavigation().stop();
                NeonArsenal.LOGGER.debug("Creeper forge: walked away for 4 s, now swelling/flashing for 1.4 s");
            }
            creeper.getNavigation().stop();
            creeper.setTarget(null);
            creeper.setSwellDir(1);
        }
    }

    /** Pulls one locked mob toward the creeper; far targets are pulled harder so they cannot unlock. */
    private static void attract(ServerLevel level, Creeper creeper, LivingEntity mob, CreeperState state) {
        Vec3 direction = new Vec3(creeper.getX() - mob.getX(), 0.0D, creeper.getZ() - mob.getZ());
        double distance = Math.sqrt(direction.lengthSqr());
        if (distance < 1.0E-4D) {
            return;
        }
        CREEPER_ATTRACTED.put(mob, state.explodeAt);
        double step;
        if (distance > 16.0D) {
            // Teleport-scale pull for escaped targets: they always catch up again.
            step = Math.min(distance - 1.0D, 1.2D);
        } else {
            step = Math.min(CREEPER_ATTRACT_STEP, distance - 1.0D);
        }
        if (step <= 0.0D) {
            return;
        }
        direction = direction.normalize().scale(step);
        mob.teleportTo(level, mob.getX() + direction.x, mob.getY(), mob.getZ() + direction.z,
                java.util.Set.of(), mob.getYRot(), mob.getXRot());
        if (mob instanceof Mob hostile) {
            hostile.setTarget(null);
            hostile.getNavigation().stop();
        }
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

    /** Summoned creepers/rabbits can never be damaged and never receive knockback. */
    public static void onKnockback(LivingKnockBackEvent event) {
        if (isForgeSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isSummonedRabbit(LivingEntity entity) {
        return entity instanceof Rabbit rabbit
                && (RABBIT_MINIONS.containsKey(rabbit)
                || rabbit.getTags().contains(TAG_SUMMONED_RABBIT)
                || rabbit.getPersistentData().contains(RABBIT_EXPIRE_KEY));
    }

    private static boolean isForgeSummon(LivingEntity entity) {
        return isSummonedRabbit(entity) || isForgeCreeper(entity);
    }

    /** Stunned mobs cannot deal damage; summoned rabbits and creepers cannot be damaged at all. */
    public static void onStunDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (isForgeSummon(event.getEntity())) {
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
        if (isDummy(entity)) {
            return false;
        }
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    /** The dummmmmmy target dummy is not a hostile target (never attracted or damaged by forge effects). */
    private static boolean isDummy(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && (id.getNamespace().equals("dummmmmmy") || id.getPath().contains("dummy"))) {
            return true;
        }
        return entity.getClass().getName().contains("dummmmmmy");
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss
                || entity.getMaxHealth() >= 200.0F;
    }
}
