package com.zszq.neonarsenal.client;

import com.zszq.neonarsenal.compat.FallSlamGlideStrikePayload;
import com.zszq.neonarsenal.compat.FallSlamPayload;
import com.zszq.neonarsenal.compat.FallSlamState;
import com.zszq.neonarsenal.compat.FallSlamStrikePayload;
import com.zszq.neonarsenal.compat.FallSlamWindupPayload;
import com.zszq.neonarsenal.compat.JumpAttackHandler;
import com.zszq.neonarsenal.mixin.MinecraftAttackAccessor;
import com.zszq.neonarsenal.weapon.WeaponDefinition.Ability;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Long-press falling slash (下落斩击).
 *
 * <p><b>Trigger.</b> Holding the attack key while falling more than five blocks for at least 0.15 s
 * (three ticks) starts the falling-slash windup. The Slow Falling effect prevents it entirely.</p>
 *
 * <p><b>Windup.</b> From the very first held tick of the long fall Better Combat's own attack path
 * is filtered: {@code startAttack}, {@code continueAttack}, its animation, its swing sounds and its
 * slash trails never run, so Better Combat can not play its own jump-slam animation. On the
 * 0.15 s mark {@link FallSlamAnimation#onExternalStart} takes over and manually plays
 * {@code bettercombat:two_handed_slam} with the player's attack cooldown as its length; the server
 * disables Better Combat's attack channel ({@code CombatFlags}). The animation freezes at one third
 * of its length while the player keeps falling.</p>
 *
 * <p><b>Commit.</b> Four blocks above the ground the animation resumes at the original speed
 * ({@code speed = 1}) and the server is told to settle the slash. The animation is never cut or
 * snapped to its last frame: it completes at original speed before or after landing.</p>
 *
 * <p><b>Recovery.</b> At the final frame the pose fades to idle over 0.3 s (six ticks, Better Combat
 * expects ticks). Attacks stay locked for exactly that fade and Better Combat's attack channel is
 * restored afterwards. A short tap instead performs one normal attack on release.</p>
 *
 * <p><b>Elytra glide.</b> Deploying an elytra during the windup keeps the frozen pose, but the slash
 * then only triggers when the gliding player hits a creature (no ground/landing trigger). Holding the
 * attack key (0.15 s) during an elytra flight with an elytra-slash weapon starts the same glide windup
 * directly, so the same collision strike applies; right-click has no effect while gliding.</p>
 */
public final class FallSlamClient {

    private static final int LONG_PRESS_TICKS = 3;   // 0.15 s
    private static final int FADE_TICKS = 6;         // 0.3 s fade to idle (Better Combat takes ticks)
    private static final int LOCK_TICKS = 6;         // 0.3 s attack lock during the fade
    private static final int ABORT_FADE_TICKS = 4;   // 0.2 s fade back to the falling pose on release
    private static final int MAX_COMMIT_TICKS = 60;  // safety net if Better Combat internals are lost
    private static final double MIN_FALL_BLOCKS = 5.0D;
    private static final double TRIGGER_BLOCKS = 4.0D;

    private enum Phase {
        /** No falling-slash activity. */
        IDLE,
        /** Attack key is held during a long fall; waiting for the 0.15 s long-press decision. */
        PENDING,
        /** Manual falling-slash windup playing, frozen at one third, Better Combat filtered. */
        WINDUP,
        /** Falling slash committed at four blocks (or on landing); animation completes at original speed. */
        COMMITTED,
        /** Final frame reached: 0.3 s fade to idle and 0.3 s attack lock. */
        LOCK
    }

    private static Phase phase = Phase.IDLE;
    private static LocalPlayer trackedPlayer;
    private static FallSlamWindSound windSound;

    private static boolean wasAttackDown;
    private static boolean glidingSlash;
    private static int keyDownTicks;
    private static int lockTicks;
    private static int commitTicks;
    private static boolean triggeringAttack;
    private static boolean serverWindup;
    private static boolean slamReported;
    /** True once this fall qualified as a long fall (>{@value #MIN_FALL_BLOCKS} blocks / above ground). */
    private static boolean longFallReached;
    /** Deferred short-fall normal attack, performed in {@link #tickPending} on the landing tick. */
    private static boolean landingNormalAttack;

    private FallSlamClient() {
    }

    /** Attack-input filter hook: true only for the short-tap normal attack started on release. */
    public static boolean isTriggeringAttack() {
        return triggeringAttack;
    }

    /**
     * Runs before vanilla processes the attack key, so the filters are in place before Better Combat
     * can start a swing in the same tick.
     */
    public static void preTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            trackedPlayer = null;
            resetLocal();
            return;
        }
        if (player != trackedPlayer) {
            trackedPlayer = player;
            resetLocal();
        }

        if (phase == Phase.COMMITTED || phase == Phase.LOCK) {
            holdFilters();
            return;
        }
        if (player.onGround() && phase != Phase.IDLE) {
            // Landing while the falling-slash input is held: keep Better Combat's attack filtered for
            // this tick (otherwise the held attack key starts a normal swing on the landing tick).
            // A real long fall commits the falling slash on landing; a short hop that never qualified
            // performs one normal attack instead. A tap released mid-air already performed its normal
            // attack through tickPending and never reaches this branch.
            holdFilters();
            if (phase == Phase.PENDING && minecraft.options.keyAttack.isDown()
                    && JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.FALL_SLASH)) {
                if (longFallReached) {
                    startWindup(player);
                } else {
                    landingNormalAttack = true;
                }
            }
            return;
        }
        if (player.isFallFlying()) {
            // Elytra gliding: the free-fall trigger never runs. Holding the attack key (0.15 s)
            // starts the same elytra-slash windup; right-click has no effect. A windup that was
            // already running keeps its frozen pose and only strikes on a creature collision (see
            // tickWindup). Weapons without the elytra-slash skill keep their normal ground-style
            // attacks while gliding (jump slam is disabled during a glide in JumpAttackHandler).
            boolean armed = JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.ELYTRA_SLASH);
            if (!armed) {
                if (phase != Phase.IDLE) {
                    FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
                    clearSlam();
                }
                return;
            }
            boolean attackDown = minecraft.options.keyAttack.isDown();
            boolean attackPressed = attackDown && !wasAttackDown;
            if (attackPressed) {
                wasAttackDown = true;
                keyDownTicks = 0;
            } else if (!attackDown) {
                wasAttackDown = false;
            }
            if (attackDown) {
                keyDownTicks++;
            }
            if (phase == Phase.IDLE) {
                if (attackPressed) {
                    phase = Phase.PENDING;
                    commitTicks = 0;
                    slamReported = false;
                    glidingSlash = true;
                    // Drop any Better Combat swing that was started before the press.
                    FallSlamAnimation.clearBetterCombatSwing();
                }
                return;
            }
            if (phase == Phase.PENDING) {
                if (attackDown) {
                    FallSlamState.attackLock = false;
                    holdFilters();
                    if (keyDownTicks >= LONG_PRESS_TICKS) {
                        startGlideWindup(player);
                    }
                    return;
                }
                // Released before the long press: one normal attack, like the falling-slash short tap.
                shortTapAttack(minecraft, player);
                return;
            }
            // WINDUP: hold the frozen 1/3 pose until a creature is hit.
            holdFilters();
            return;
        }
        if (player.hasEffect(MobEffects.SLOW_FALLING)) {
            // The Slow Falling effect prevents the falling slash entirely; an already started
            // windup is retracted as well.
            if (phase == Phase.WINDUP) {
                FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
            }
            if (phase != Phase.IDLE) {
                clearSlam();
            }
            return;
        }

        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (!attackDown) {
            wasAttackDown = false;
            return;
        }
        if (!wasAttackDown) {
            wasAttackDown = true;
            keyDownTicks = 0;
        }
        keyDownTicks++;

        if (player.onGround() || player.getAbilities().flying
                || !JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.FALL_SLASH)) {
            return;
        }
        // "Potential falling slash": the ground is more than five blocks below (even while still
        // rising out of a jump). The press must not start a full jump-slam swing in that case -
        // holding the key turns into the falling slash, tapping still performs the normal attack.
        boolean overLongDrop = heightToGround(player) > MIN_FALL_BLOCKS;
        // A long fall (the falling-slash condition from the spec): already fell more than five
        // blocks, or descending with the ground more than five blocks below.
        boolean longFall = player.fallDistance > MIN_FALL_BLOCKS
                || (player.getDeltaMovement().y <= 0.0D && overLongDrop);
        // A real short jump keeps its attack input (jump slam); only a potential long drop waits.
        if (phase == Phase.IDLE && !overLongDrop && player.fallDistance <= MIN_FALL_BLOCKS
                && JumpAttackHandler.isJumpAttackAirborne(player)) {
            return;
        }
        if (!overLongDrop && player.fallDistance <= MIN_FALL_BLOCKS) {
            return;
        }
        longFallReached |= longFall;

        if (phase == Phase.IDLE) {
            phase = Phase.PENDING;
            commitTicks = 0;
            slamReported = false;
            // Drop any Better Combat swing that was started before the long fall was detected. Its
            // delayed hit, sound and trail must not leak into the windup.
            FallSlamAnimation.clearBetterCombatSwing();
        }
        // Enter the 1/3 windup once the long press is confirmed AND the fall actually qualifies as a
        // long fall. A tap (released before the long press) stays a normal attack; it must not be
        // converted into a falling slash merely because the player is inside the trigger radius.
        if (phase == Phase.PENDING && keyDownTicks >= LONG_PRESS_TICKS && longFall) {
            startWindup(player);
        }
        if (phase == Phase.PENDING || phase == Phase.WINDUP) {
            holdFilters();
        }
    }

    /** Runs after the player has been ticked, so positions and the animation are up to date. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            trackedPlayer = null;
            resetLocal();
            return;
        }
        if (player != trackedPlayer) {
            trackedPlayer = player;
            resetLocal();
        }
        if (player.isDeadOrDying()) {
            abort(player);
            return;
        }
        updateWind(player);

        switch (phase) {
            case LOCK -> tickLock(player);
            case COMMITTED -> tickCommitted(player);
            case WINDUP -> tickWindup(player, minecraft);
            case PENDING -> tickPending(player, minecraft);
            case IDLE -> clearFilters();
        }
    }

    private static void startWindup(LocalPlayer player) {
        phase = Phase.WINDUP;
        commitTicks = 0;
        slamReported = false;
        glidingSlash = false;
        holdFilters();
        FallSlamState.attackLock = false;
        FallSlamAnimation.clearBetterCombatSwing();
        FallSlamAnimation.onExternalStart(player);
        // Disable Better Combat's attack channel on the server too.
        setServerWindup(true);
    }

    /** Starts the same frozen windup from an elytra glide attack hold; the strike waits for impact. */
    private static void startGlideWindup(LocalPlayer player) {
        phase = Phase.WINDUP;
        commitTicks = 0;
        slamReported = false;
        glidingSlash = true;
        holdFilters();
        FallSlamState.attackLock = false;
        FallSlamAnimation.clearBetterCombatSwing();
        FallSlamAnimation.onExternalStart(player);
        setServerWindup(true);
    }

    private static void tickPending(LocalPlayer player, Minecraft minecraft) {
        if (glidingSlash) {
            // Elytra-slash pending is advanced by preTick every tick; just keep the filters in place.
            FallSlamState.attackLock = false;
            holdFilters();
            return;
        }
        if (landingNormalAttack) {
            // A short hop that never qualified as a long fall: perform the deferred normal attack
            // instead of turning it into a falling slash.
            landingNormalAttack = false;
            shortTapAttack(minecraft, player);
            return;
        }
        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean airborne = !player.onGround() && !player.getAbilities().flying;
        boolean armed = JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.FALL_SLASH);
        if (attackDown && airborne && armed) {
            // Still waiting for the 0.15 s long-press decision; preTick advances it.
            FallSlamState.attackLock = false;
            holdFilters();
            return;
        }
        if (!attackDown && armed && keyDownTicks < LONG_PRESS_TICKS) {
            // Short tap: one normal attack on release. This also covers a tap whose release lands on
            // the same tick (the attack must not be swallowed by the landing).
            shortTapAttack(minecraft, player);
            return;
        }
        clearSlam();
    }

    private static void tickWindup(LocalPlayer player, Minecraft minecraft) {
        FallSlamState.attackLock = false;
        holdFilters();

        if (player.isFallFlying()) {
            // Gliding windup: keep the frozen 1/3 pose and wait for a creature collision instead of
            // the normal four-block / landing trigger. Only weapons with the elytra-slash skill may
            // keep or start this windup.
            if (!JumpAttackHandler.isHoldingAbilityWeapon(player, Ability.ELYTRA_SLASH)) {
                FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
                clearSlam();
                return;
            }
            glidingSlash = true;
            FallSlamAnimation.tickPause(player);
            LivingEntity target = findGlideTarget(player);
            if (target != null) {
                glideStrike(player, target);
            }
            return;
        }
        if (glidingSlash) {
            // The glide ended (landed, water, ...) without hitting a creature: no strike.
            FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
            clearSlam();
            return;
        }

        boolean landed = player.onGround();
        boolean trigger = landed || player.getAbilities().flying
                || heightToGround(player) <= TRIGGER_BLOCKS;
        if (trigger) {
            commit(player);
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            // Released before the slam committed: fade back and reset the measured fall height.
            FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
            clearSlam();
            return;
        }
        FallSlamAnimation.tickPause(player);
    }

    /** {@return the first living creature the gliding player collided with, or {@code null}}. */
    private static LivingEntity findGlideTarget(LocalPlayer player) {
        AABB area = player.getBoundingBox()
                .expandTowards(player.getDeltaMovement())
                .inflate(0.6D);
        for (Entity entity : player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
                        && !(e instanceof Player) && !(e instanceof ArmorStand))) {
            return (LivingEntity) entity;
        }
        return null;
    }

    /** Triggers the slash on the creature hit during the glide. */
    private static void glideStrike(LocalPlayer player, LivingEntity target) {
        phase = Phase.COMMITTED;
        commitTicks = 0;
        slamReported = true;
        holdFilters();
        FallSlamState.attackLock = false;
        // Continue from the frozen 1/3 pose at original speed; the strike sound plays together with
        // it and the server applies the speed-scaled damage to the hit creature.
        FallSlamAnimation.resumeOriginal(player);
        send(new FallSlamStrikePayload());
        // The glide slash extra damage follows the impact speed; the server converts it to the
        // equivalent free-fall height so it stays balanced with the normal falling slash.
        send(new FallSlamGlideStrikePayload(target.getId(), (float) player.getDeltaMovement().length()));
    }

    private static void commit(LocalPlayer player) {
        phase = Phase.COMMITTED;
        commitTicks = 0;
        holdFilters();
        FallSlamState.attackLock = false;
        // Original speed: continue the whole animation without accelerating or snapping it.
        FallSlamAnimation.resumeOriginal(player);
        if (!slamReported) {
            slamReported = true;
            // The slam just resumed from its frozen 1/3 pose: play the claymore's third attack swing
            // sound now so it matches the actual chopping animation (no longer at the animation end).
            send(new FallSlamStrikePayload());
            // The fall height always counts from the start of the current fall, including any part
            // of it spent with the windup cancelled: the landing estimate is the fall distance so
            // far (vanilla fall distance) plus the distance still to the ground.
            double totalFall = Math.max(0.0D, player.fallDistance) + heightToGround(player);
            send(new FallSlamPayload((float) totalFall));
        }
    }

    private static void tickCommitted(LocalPlayer player) {
        FallSlamState.attackLock = false;
        holdFilters();
        commitTicks++;
        if (commitTicks > MAX_COMMIT_TICKS || FallSlamAnimation.isFinished(player)) {
            beginFade(player);
        }
    }

    private static void beginFade(LocalPlayer player) {
        phase = Phase.LOCK;
        lockTicks = LOCK_TICKS;
        holdFilters();
        FallSlamState.attackLock = true;
        // Fade to idle over 0.3 s (6 ticks).
        FallSlamAnimation.finishAndFade(player, FADE_TICKS);
    }

    private static void tickLock(LocalPlayer player) {
        holdFilters();
        FallSlamState.attackLock = true;
        lockTicks--;
        if (lockTicks <= 0) {
            restoreAfterSlam();
        }
    }

    /** Restores Better Combat attacks after the 0.3 s lock that matches the fade. */
    private static void restoreAfterSlam() {
        resetState();
        clearFilters();
        setServerWindup(false);
        // The wind sound is not stopped here: it belongs to the actual fall, so it must keep
        // playing (without restarting) until the fall itself ends (see updateWind).
        // Keep the key edge in sync: if attack is still held after the lock, it must not count as a
        // new press (otherwise the elytra slash would chain-strike while the key stays down).
        wasAttackDown = attackKeyDown();
        keyDownTicks = 0;
    }

    private static void abort(LocalPlayer player) {
        if (phase == Phase.WINDUP || phase == Phase.COMMITTED) {
            FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
        } else if (phase == Phase.PENDING) {
            FallSlamAnimation.clearBetterCombatSwing();
        }
        clearSlam();
        stopWind();
    }

    private static void shortTapAttack(Minecraft minecraft, LocalPlayer player) {        // Let this one normal attack through the filters: Better Combat may pick its airborne slam
        // for it, which is exactly the short-tap behaviour.
        FallSlamState.waitingForSlam = false;
        FallSlamState.blockAttacks = false;
        FallSlamState.allowNormalAttack = true;
        triggeringAttack = true;
        try {
            ((MinecraftAttackAccessor) minecraft).neonarsenal$startAttack();
        } catch (Throwable ignored) {
            // Optional normal attack fallback.
        } finally {
            triggeringAttack = false;
            FallSlamState.allowNormalAttack = false;
        }
        clearSlam();
    }

    /** {@return whether the local attack key is currently held}. */
    private static boolean attackKeyDown() {
        return Minecraft.getInstance().options.keyAttack.isDown();
    }

    /** Ends any active falling-slash state and restores Better Combat's attack channel. */
    private static void clearSlam() {
        resetState();
        clearFilters();
        setServerWindup(false);
        // Do not stop the wind here: cancelling the windup does not end the fall, so the same wind
        // instance keeps playing instead of restarting from silence on the next attack press.
        wasAttackDown = attackKeyDown();
        keyDownTicks = 0;
    }

    /** Drops the local state without touching the server or Better Combat (used on player change). */
    private static void resetLocal() {
        resetState();
        clearFilters();
        FallSlamAnimation.reset();
        stopWind();
        serverWindup = false;
        wasAttackDown = false;
        keyDownTicks = 0;
    }

    private static void resetState() {
        phase = Phase.IDLE;
        lockTicks = 0;
        commitTicks = 0;
        slamReported = false;
        glidingSlash = false;
        longFallReached = false;
        landingNormalAttack = false;
        FallSlamState.attackLock = false;
    }

    private static void clearFilters() {
        FallSlamState.blockAttacks = false;
        FallSlamState.waitingForSlam = false;
        FallSlamState.allowAnimation = false;
        FallSlamState.allowNormalAttack = false;
    }

    private static void holdFilters() {
        FallSlamState.blockAttacks = true;
        FallSlamState.waitingForSlam = true;
    }

    private static void setServerWindup(boolean windup) {
        if (serverWindup == windup) {
            return;
        }
        serverWindup = windup;
        send(new FallSlamWindupPayload(windup));
    }

    private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        try {
            PacketDistributor.sendToServer(payload);
        } catch (Throwable ignored) {
            // Not connected (single-player transition, disconnect); nothing to restore remotely.
        }
    }

    /**
     * Keeps the looping elytra gliding wind alive while the player is falling. Volume and pitch are
     * updated by {@link FallSlamWindSound} from the current fall speed.
     *
     * <p>The wind starts once the player is in a real fall of more than three blocks (or inside the
     * falling-slash windup) and then keeps playing for the rest of that fall: cancelling or
     * re-pressing the attack, or finishing the slam, must not restart it. Slow Falling counts as a
     * gentle descent, so it does not play the wind.</p>
     */
    private static void updateWind(LocalPlayer player) {
        boolean fallEnded = player.onGround() || player.getAbilities().flying
                || player.isFallFlying() || player.isInWater()
                || player.hasEffect(MobEffects.SLOW_FALLING);
        if (fallEnded) {
            stopWind();
            return;
        }
        boolean started = windSound != null && !windSound.isStopped();
        if (!started && (phase != Phase.IDLE || player.fallDistance > 3.0F)) {
            windSound = new FallSlamWindSound(player);
            Minecraft.getInstance().getSoundManager().play(windSound);
        }
    }

    private static void stopWind() {
        if (windSound != null) {
            windSound.stopWind();
            windSound = null;
        }
    }

    private static double heightToGround(LocalPlayer player) {
        Vec3 start = player.position();
        BlockHitResult hit = player.level().clip(new ClipContext(start, start.add(0.0D, -64.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return 64.0D;
        }
        return Math.max(0.0D, start.y - hit.getLocation().y);
    }
}
