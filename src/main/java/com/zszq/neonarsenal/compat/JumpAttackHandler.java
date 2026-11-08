package com.zszq.neonarsenal.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.registry.ModItems;
import com.zszq.neonarsenal.weapon.WeaponCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Jump attack override for Better Combat.
 *
 * <p>Better Combat has no "player is airborne" attack condition, so a small mixin on its attack
 * selection ({@code PlayerAttackHelper#getCurrentAttack}) calls into this class. When the player is
 * airborne and holds one of our weapons, every weapon plays the <b>claymore's last attack</b> (the
 * overhead slam) instead of its own attack, while the swing/impact sounds stay the weapon's own.
 * The combo counter is pushed to {@code -1} so Better Combat's next increment lands on {@code 0}:
 * every jump attack therefore interrupts the chain and the next ground attack starts from the first
 * combo step.</p>
 *
 * <p>Better Combat is optional, so everything is reached reflectively; if anything is missing the
 * handler simply returns {@code null} and normal Better Combat behaviour is kept.</p>
 */
public final class JumpAttackHandler {

    private static boolean initTried;
    private static boolean ready;
    private static final java.util.Map<java.util.UUID, Integer> RECENT_JUMP_SLAM =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int JUMP_SLAM_TICKS = 5;

    private static Method getAttributes;
    private static Method attacksMethod;
    private static Method isDualWieldingMethod;
    private static Method shouldAttackWithOffHandMethod;
    private static Method setComboCountMethod;
    private static Method animationMethod;
    private static Method conditionsMethod;
    private static Method hitboxMethod;
    private static Method damageMultiplierMethod;
    private static Method movementSpeedMultiplierMethod;
    private static Method rangeMultiplierMethod;
    private static Method angleMethod;
    private static Method upswingMethod;
    private static Method swingSoundMethod;
    private static Method impactSoundMethod;
    private static Method trailParticlesMethod;
    private static Constructor<?> attackHandConstructor;
    private static Constructor<?> comboStateConstructor;
    private static Constructor<?> attackConstructor;

    private static Object claymoreSlam;
    private static int claymoreAttackCount;

    /** Per weapon id: claymore slam animation with that weapon's own swing/impact sound. */
    private static final Map<String, Object> JUMP_ATTACKS = new HashMap<>();

    /** Wall-clock time (ms) until which jumping is blocked after a jump attack (first cooldown half). */
    private static final Map<UUID, Long> JUMP_BLOCK_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    /** Players that have jumped and have not landed yet; jump attacks are allowed for this whole window. */
    private static final java.util.Set<UUID> JUMP_AIRBORNE =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static Method attackCooldownMethod;

    private static boolean logged;

    private JumpAttackHandler() {
    }

    /**
     * {@return a forced attack hand using the claymore slam (with the weapon's own sound), or
     * {@code null} to let Better Combat select its normal attack}.
     *
     * @param player     the player that is about to attack
     * @param comboCount Better Combat's current combo counter
     */
    public static Object forPlayer(Player player, int comboCount) {
        if (player == null || player.onGround() || player.getAbilities().flying) {
            // Creative flight attacks use the normal ground attack, not the jump slam.
            return null;
        }
        if (player.isFallFlying()) {
            // Elytra gliding keeps the normal ground-style attack. The elytra slash (long attack
            // press) is driven by FallSlamClient; a jump slam must never fire while gliding.
            return null;
        }
        // Vanilla-style airborne attack conditions: any time between jumping and landing counts,
        // including while falling. Climbing, swimming, riding and blindness are excluded just like
        // vanilla critical hits.
        if (!isJumpAttackAirborne(player)) {
            return null;
        }
        if (player.fallDistance > 5.0F) {
            // A sustained fall belongs to the falling slash (for its weapons) or to plain ground-style
            // attacks (for every other weapon); it must not become a jump slam.
            return null;
        }
        if (player.onClimbable() || player.isInWater() || player.isPassenger()
                || player.hasEffect(MobEffects.BLINDNESS)) {
            return null;
        }
        // Only actual attack input may turn into a jump attack. Better Combat also queries the
        // current attack for animation/pose code while the player is merely jumping; without this
        // guard a normal jump would already start the jump-attack cooldown and block jumping.
        if (player.level().isClientSide() && !isAttackInputActive(player)) {
            return null;
        }
        if (player.level().isClientSide()
                && com.zszq.neonarsenal.compat.FallSlamState.allowNormalAttack) {
            // A short tap during a long fall is a normal attack, not the jump slam.
            return null;
        }
        init();
        if (!ready) {
            return null;
        }
        try {
            ItemStack target = player.getMainHandItem();
            boolean offHand = false;
            String weaponId = arsenalId(target);
            if (weaponId == null) {
                ItemStack offhand = player.getOffhandItem();
                String offhandId = arsenalId(offhand);
                if (offhandId == null
                        || !Boolean.TRUE.equals(isDualWieldingMethod.invoke(null, player))
                        || !Boolean.TRUE.equals(shouldAttackWithOffHandMethod.invoke(null, player, comboCount))) {
                    return null;
                }
                target = offhand;
                weaponId = offhandId;
                offHand = true;
            }
            // Weapons without the jump-attack skill keep Better Combat's normal airborne attack.
            if (!canWeapon(weaponId, com.zszq.neonarsenal.weapon.WeaponDefinition.Ability.JUMP_ATTACK)) {
                return null;
            }
            Object attributes = getAttributes.invoke(null, target);
            if (attributes == null) {
                return null;
            }
            Object jumpAttack = jumpAttackFor(target, weaponId);
            if (jumpAttack == null) {
                return null;
            }
            Object comboState = comboStateConstructor.newInstance(
                    Math.max(0, claymoreAttackCount - 1), claymoreAttackCount);
            Object attackHand = attackHandConstructor.newInstance(
                    jumpAttack, comboState, offHand, attributes, target);
            // -1 + Better Combat's per-attack increment = 0, so the next ground attack starts at the
            // first combo step: every jump attack resets the chain.
            try {
                setComboCountMethod.invoke(player, -1);
            } catch (Throwable ignored) {
                // Combo reset is a nicety; the forced attack still works without it.
            }
            if (!logged) {
                logged = true;
                NeonArsenal.LOGGER.info(
                        "Jump attack enabled: airborne attacks play the claymore slam with the weapon's own sound");
            }
            if (player.level().isClientSide()) {
                // Only the client tracks the jump cooldown; Better Combat also queries the current
                // attack for animation/pose updates on the server, which must not start a cooldown.
                markJumpAttack(player);
            }
            RECENT_JUMP_SLAM.put(player.getUUID(), player.tickCount);
            return attackHand;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** True when the player performed a jump-slam attack within the last few ticks. */
    public static boolean isRecentJumpSlam(Player player) {
        if (player == null) {
            return false;
        }
        Integer tick = RECENT_JUMP_SLAM.get(player.getUUID());
        return tick != null && player.tickCount - tick <= JUMP_SLAM_TICKS;
    }

    /** {@return the item id when the stack is one of our weapons, otherwise {@code null}}. */
    public static String arsenalId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(NeonArsenal.MODID) || WeaponCatalog.find(id.getPath()).isEmpty()) {
            return null;
        }
        return id.getPath();
    }

    public static boolean isHoldingArsenalWeapon(Player player) {
        return player != null
                && (arsenalId(player.getMainHandItem()) != null || arsenalId(player.getOffhandItem()) != null);
    }

    /** {@return whether the catalog weapon with this id may perform the given skill}. */
    private static boolean canWeapon(String weaponId,
                                     com.zszq.neonarsenal.weapon.WeaponDefinition.Ability ability) {
        return weaponId != null && WeaponCatalog.find(weaponId)
                .map(definition -> definition.can(ability))
                .orElse(false);
    }

    /** {@return whether the stack is one of our weapons with the given skill}. */
    public static boolean hasAbility(ItemStack stack,
                                     com.zszq.neonarsenal.weapon.WeaponDefinition.Ability ability) {
        return canWeapon(arsenalId(stack), ability);
    }

    /** {@return whether the player's main-hand weapon has the given skill}. */
    public static boolean isHoldingAbilityWeapon(Player player,
                                                 com.zszq.neonarsenal.weapon.WeaponDefinition.Ability ability) {
        return player != null && hasAbility(player.getMainHandItem(), ability);
    }

    /** {@return whether jumping is currently blocked by the jump-attack cooldown (first half)}. */
    public static boolean isJumpBlocked(Player player) {
        if (player == null) {
            return false;
        }
        Long until = JUMP_BLOCK_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            JUMP_BLOCK_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /**
     * Records actual jumps so only a real jump attack (not merely attacking while falling) starts
     * the jump cooldown.
     */
    public static void onLivingJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            JUMP_AIRBORNE.add(player.getUUID());
        }
    }

    /** Clears the jump window once the player is back on the ground. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.onGround()) {
            JUMP_AIRBORNE.remove(player.getUUID());
        }
    }

    /** {@return true from a real jump until landing (vanilla-style airborne attack window)}. */
    public static boolean isJumpAttackAirborne(Player player) {
        return player != null && !player.onGround() && JUMP_AIRBORNE.contains(player.getUUID());
    }

    /**
     * Starts the jump-attack cooldown. Its length follows the weapon's Better Combat attack cooldown
     * (at least 10 ticks). Only the <b>first half</b> of that cooldown blocks jumping, and only if
     * the player actually jumped recently (an attack while simply falling is not a jump attack and
     * never blocks jumping). Attacks are never affected.
     */
    public static void markJumpAttack(Player player) {
        if (player == null) {
            return;
        }
        if (!isJumpAttackAirborne(player)) {
            return;
        }
        init();
        int cooldownTicks = 12;
        if (attackCooldownMethod != null) {
            try {
                double cooldown = ((Number) attackCooldownMethod.invoke(null, player)).doubleValue();
                cooldownTicks = Math.max(10, (int) Math.ceil(cooldown));
            } catch (Throwable ignored) {
                // Fall back to the default cooldown above.
            }
        }
        int blockedTicks = Math.max(1, (cooldownTicks + 1) / 2);
        JUMP_BLOCK_UNTIL.put(player.getUUID(), System.currentTimeMillis() + blockedTicks * 50L);
        NeonArsenal.LOGGER.debug(
                "Jump-attack cooldown: jump blocked for {} of {} ticks", blockedTicks, cooldownTicks);
    }

    /** Logs whether the Better Combat mixin was actually applied. */
    public static void reportStatus() {
        try {
            Class<?> helper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
            boolean applied = false;
            for (java.lang.reflect.Method method : helper.getDeclaredMethods()) {
                if (method.getName().contains("bettercombat_neonarsenal")) {
                    applied = true;
                    break;
                }
            }
            NeonArsenal.LOGGER.info("Jump-attack mixin applied: {}", applied);
            init();
            NeonArsenal.LOGGER.info("Jump-attack reflection ready: {}", ready);
            try {
                Class<?> livingEntity = Class.forName("net.minecraft.world.entity.LivingEntity");
                boolean jumpBlocked = false;
                for (java.lang.reflect.Method method : livingEntity.getDeclaredMethods()) {
                    if (method.getName().contains("bettercombat_neonarsenal$blockJump")) {
                        jumpBlocked = true;
                        break;
                    }
                }
                NeonArsenal.LOGGER.info("Jump-attack jump blocking mixin applied: {}", jumpBlocked);
            } catch (Throwable ignored) {
                NeonArsenal.LOGGER.info("Jump-attack jump blocking mixin applied: false");
            }
        } catch (Throwable throwable) {
            NeonArsenal.LOGGER.info("Better Combat not present; jump-attack override disabled");
        }
    }

    private static boolean isAttackInputActive(Player player) {
        try {
            if (player.swinging) {
                return true;
            }
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object options = minecraftClass.getField("options").get(minecraft);
            if (options == null) {
                return true;
            }
            Object keyAttack = options.getClass().getField("keyAttack").get(options);
            return keyAttack != null && Boolean.TRUE.equals(
                    keyAttack.getClass().getMethod("isDown").invoke(keyAttack));
        } catch (Throwable ignored) {
            // If client classes are unavailable for any reason, keep the old behaviour.
            return true;
        }
    }

    private static Object jumpAttackFor(ItemStack weapon, String weaponId) {
        Object cached = JUMP_ATTACKS.get(weaponId);
        if (cached != null) {
            return cached;
        }
        Object slam = claymoreSlam();
        if (slam == null) {
            return null;
        }
        try {
            Object attributes = getAttributes.invoke(null, weapon);
            if (attributes == null) {
                // Weapon attributes are not loaded yet; do not cache a half-built attack.
                return null;
            }
            Object ownSound = null;
            Object ownImpact = null;
            Object[] attacks = (Object[]) attacksMethod.invoke(attributes);
            if (attacks != null && attacks.length > 0) {
                Object ownAttack = attacks[attacks.length - 1];
                ownSound = swingSoundMethod.invoke(ownAttack);
                ownImpact = impactSoundMethod.invoke(ownAttack);
            }
            if (ownSound == null) {
                ownSound = swingSoundMethod.invoke(slam);
            }
            if (ownImpact == null) {
                ownImpact = impactSoundMethod.invoke(slam);
            }
            Object jumpAttack = attackConstructor.newInstance(
                    conditionsMethod.invoke(slam),
                    hitboxMethod.invoke(slam),
                    damageMultiplierMethod.invoke(slam),
                    movementSpeedMultiplierMethod.invoke(slam),
                    rangeMultiplierMethod.invoke(slam),
                    angleMethod.invoke(slam),
                    upswingMethod.invoke(slam),
                    animationMethod.invoke(slam),
                    ownSound,
                    ownImpact,
                    trailParticlesMethod.invoke(slam));
            JUMP_ATTACKS.put(weaponId, jumpAttack);
            return jumpAttack;
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static Object claymoreSlam() {
        if (claymoreSlam != null) {
            return claymoreSlam;
        }
        try {
            ItemStack claymore = new ItemStack(ModItems.get("claymore").get());
            Object attributes = getAttributes.invoke(null, claymore);
            if (attributes == null) {
                return null;
            }
            Object[] attacks = (Object[]) attacksMethod.invoke(attributes);
            if (attacks == null || attacks.length == 0) {
                return null;
            }
            claymoreSlam = attacks[attacks.length - 1];
            claymoreAttackCount = attacks.length;
            return claymoreSlam;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static synchronized void init() {
        if (initTried) {
            return;
        }
        initTried = true;
        try {
            Class<?> helper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
            Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
            Class<?> attributes = Class.forName("net.bettercombat.api.WeaponAttributes");
            Class<?> attack = Class.forName("net.bettercombat.api.WeaponAttributes$Attack");
            Class<?> comboState = Class.forName("net.bettercombat.api.ComboState");
            Class<?> attackHand = Class.forName("net.bettercombat.api.AttackHand");
            Class<?> attackProperties = Class.forName("net.bettercombat.logic.PlayerAttackProperties");

            getAttributes = registry.getMethod("getAttributes", ItemStack.class);
            attacksMethod = attributes.getMethod("attacks");
            isDualWieldingMethod = helper.getMethod("isDualWielding", Player.class);
            shouldAttackWithOffHandMethod = helper.getMethod("shouldAttackWithOffHand", Player.class, int.class);
            attackCooldownMethod = helper.getMethod("getAttackCooldownTicksCapped", Player.class);
            setComboCountMethod = attackProperties.getMethod("setComboCount", int.class);

            animationMethod = attack.getMethod("animation");
            conditionsMethod = attack.getMethod("conditions");
            hitboxMethod = attack.getMethod("hitbox");
            damageMultiplierMethod = attack.getMethod("damageMultiplier");
            movementSpeedMultiplierMethod = attack.getMethod("movementSpeedMultiplier");
            rangeMultiplierMethod = attack.getMethod("rangeMultiplier");
            angleMethod = attack.getMethod("angle");
            upswingMethod = attack.getMethod("upswing");
            swingSoundMethod = attack.getMethod("swingSound");
            impactSoundMethod = attack.getMethod("impactSound");
            trailParticlesMethod = attack.getMethod("trailParticles");

            attackHandConstructor = attackHand.getConstructor(
                    attack, comboState, boolean.class, attributes, ItemStack.class);
            comboStateConstructor = comboState.getConstructor(int.class, int.class);
            attackConstructor = attack.getConstructor(
                    conditionsMethod.getReturnType(),
                    hitboxMethod.getReturnType(),
                    double.class,
                    float.class,
                    float.class,
                    double.class,
                    double.class,
                    String.class,
                    swingSoundMethod.getReturnType(),
                    impactSoundMethod.getReturnType(),
                    trailParticlesMethod.getReturnType());
            ready = true;
        } catch (Throwable throwable) {
            ready = false;
            NeonArsenal.LOGGER.debug("Better Combat jump-attack integration is unavailable", throwable);
        }
    }
}
