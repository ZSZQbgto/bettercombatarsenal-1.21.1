package com.zszq.neonarsenal.client;

import com.zszq.neonarsenal.compat.FallSlamGlideStrikePayload;
import com.zszq.neonarsenal.compat.FallSlamPayload;
import com.zszq.neonarsenal.compat.FallSlamState;
import com.zszq.neonarsenal.compat.FallSlamStrikePayload;
import com.zszq.neonarsenal.compat.FallSlamWindupPayload;
import com.zszq.neonarsenal.compat.JumpAttackHandler;
import com.zszq.neonarsenal.mixin.MinecraftAttackAccessor;

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
 * then only triggers when the gliding player hits a creature (no ground/landing trigger). Right-click
 * during an elytra flight with an arsenal weapon starts the same glide windup directly, so the same
 * collision strike applies.</p>
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
    private static boolean wasUseDown;
    private static boolean glidingSlash;
    private static int keyDownTicks;
    private static int lockTicks;
    private static int commitTicks;
    private static boolean triggeringAttack;
    private static boolean serverWindup;
    private static boolean slamReported;
    private static boolean afterRelease;
    private static double fallStartY = Double.NaN;

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

        boolean useDown = minecraft.options.keyUse.isDown();
        boolean usePressed = useDown && !wasUseDown;
        wasUseDown = useDown;

        if (phase == Phase.COMMITTED || phase == Phase.LOCK) {
            holdFilters();
            return;
        }
        if (player.isFallFlying()) {
            // Elytra gliding: the normal free-fall trigger never runs. Right-click (while holding an
            // arsenal weapon) starts the same windup; a windup that was already running keeps its
            // frozen pose. The slash itself only triggers on a creature collision (see tickWindup).
            wasAttackDown = false;
            if (phase == Phase.IDLE) {
                if (usePressed && JumpAttackHandler.isHoldingArsenalWeapon(player)) {
                    startGlideWindup(player, minecraft);
                }
            } else if (phase == Phase.PENDING) {
                // The elytra replaced the free fall before the long press completed.
                clearSlam();
            } else {
                holdFilters();
            }
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
                || !JumpAttackHandler.isHoldingArsenalWeapon(player)) {
            return;
        }
        if (Double.isNaN(fallStartY)) {
            fallStartY = afterRelease ? player.getY() : player.getY() + player.fallDistance;
            afterRelease = false;
        }
        boolean longFall = fallStartY - player.getY() > MIN_FALL_BLOCKS;
        if (!longFall) {
            // A descending player that is more than five blocks above the ground will fall far
            // enough for a falling slash. From the press on, treat the input as a falling-slash hold
            // instead of letting Better Combat start its own airborne jump-slam animation; the
            // windup then starts after the 0.15 s long press and no Better Combat jump-slam animation
            // plays.
            longFall = player.getDeltaMovement().y <= 0.0D
                    && heightToGround(player) > MIN_FALL_BLOCKS;
        }
        if (!longFall) {
            return;
        }

        if (phase == Phase.IDLE) {
            phase = Phase.PENDING;
            commitTicks = 0;
            slamReported = false;
            // Drop any Better Combat swing that was started before the long fall was detected. Its
            // delayed hit, sound and trail must not leak into the windup.
            FallSlamAnimation.clearBetterCombatSwing();
        }
        if (phase == Phase.PENDING && keyDownTicks >= LONG_PRESS_TICKS) {
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

    /** Starts the same frozen windup from an elytra flight right-click; the strike waits for impact. */
    private static void startGlideWindup(LocalPlayer player, Minecraft minecraft) {
        phase = Phase.WINDUP;
        commitTicks = 0;
        slamReported = false;
        glidingSlash = true;
        afterRelease = false;
        // Keep the fall origin for the height-scaled damage (elytra descent counts as the fall).
        fallStartY = player.getY() + player.fallDistance;
        holdFilters();
        FallSlamState.attackLock = false;
        FallSlamAnimation.clearBetterCombatSwing();
        FallSlamAnimation.onExternalStart(player);
        setServerWindup(true);
        // This right-click belongs to the glide slash, not to item use / blocking.
        minecraft.options.keyUse.consumeClick();
    }

    private static void tickPending(LocalPlayer player, Minecraft minecraft) {
        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean airborne = !player.onGround() && !player.getAbilities().flying;
        boolean armed = JumpAttackHandler.isHoldingArsenalWeapon(player);
        if (attackDown && airborne && armed) {
            // Still waiting for the 0.15 s long-press decision; preTick advances it.
            FallSlamState.attackLock = false;
            holdFilters();
            return;
        }
        if (!attackDown && airborne && armed && keyDownTicks < LONG_PRESS_TICKS) {
            // Short tap: one normal attack on release.
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
            // the normal four-block / landing trigger.
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
            double fall = Double.isNaN(fallStartY)
                    ? Math.max(0.0D, player.fallDistance)
                    : Math.max(0.0D, fallStartY - player.getY());
            send(new FallSlamPayload((float) fall));
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
        stopWind();
        afterRelease = true;
        fallStartY = Double.NaN;
        wasAttackDown = false;
        keyDownTicks = 0;
    }

    private static void abort(LocalPlayer player) {
        if (phase == Phase.WINDUP || phase == Phase.COMMITTED) {
            FallSlamAnimation.stop(player, ABORT_FADE_TICKS);
        } else if (phase == Phase.PENDING) {
            FallSlamAnimation.clearBetterCombatSwing();
        }
        clearSlam();
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

    /** Ends any active falling-slash state and restores Better Combat's attack channel. */
    private static void clearSlam() {
        resetState();
        clearFilters();
        setServerWindup(false);
        stopWind();
        afterRelease = true;
        fallStartY = Double.NaN;
        wasAttackDown = false;
        keyDownTicks = 0;
    }

    /** Drops the local state without touching the server or Better Combat (used on player change). */
    private static void resetLocal() {
        resetState();
        clearFilters();
        FallSlamAnimation.reset();
        stopWind();
        serverWindup = false;
        afterRelease = true;
        fallStartY = Double.NaN;
        wasAttackDown = false;
        wasUseDown = false;
        keyDownTicks = 0;
    }

    private static void resetState() {
        phase = Phase.IDLE;
        lockTicks = 0;
        commitTicks = 0;
        slamReported = false;
        glidingSlash = false;
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
     * <p>Active during the whole falling-slash descent, and also for any other real fall of more than
     * three blocks so the wind matches the "falling" state instead of only the slash states. Slow
     * Falling counts as a gentle descent, so it does not play the wind.</p>
     */
    private static void updateWind(LocalPlayer player) {
        boolean falling = !player.onGround() && !player.getAbilities().flying
                && !player.isFallFlying() && !player.isInWater()
                && !player.hasEffect(MobEffects.SLOW_FALLING)
                && (phase != Phase.IDLE || player.fallDistance > 3.0F);
        if (!falling) {
            stopWind();
            return;
        }
        if (windSound == null || windSound.isStopped()) {
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
