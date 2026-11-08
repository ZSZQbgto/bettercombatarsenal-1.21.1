package com.zszq.neonarsenal.compat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zszq.neonarsenal.item.Sharpness;
import com.zszq.neonarsenal.item.SharpnessWeaponItem;
import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Right-click blocking with the arsenal weapons.
 *
 * <p>Holding right-click starts the item use state; every successfully blocked hit cancels the
 * damage and costs one sharpness point. Blocking for more than 2 s breaks the Better Combat combo
 * when released, while shorter blocks keep the chain alive.</p>
 */
public final class BlockHandler {

    private static final int LONG_BLOCK_TICKS = 40;
    private static final Map<UUID, Long> BLOCK_START = new HashMap<>();

    private static Method setComboCountMethod;
    private static boolean comboLookupTried;

    private BlockHandler() {
    }

    /** {@return true while the player is holding right-click with an arsenal weapon}. */
    public static boolean isBlocking(net.minecraft.world.entity.player.Player player) {
        return player != null
                && player.isUsingItem()
                && player.getUseItem().getItem() instanceof SharpnessWeaponItem;
    }

    /** Successful block: cancel the hit and spend one sharpness point. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isBlocking(player)) {
            return;
        }
        // Follow the vanilla hurt path: any incoming damage event that would have damaged the
        // player while blocking counts as exactly one successful block.
        if (event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        // 210 degree frontal arc: attacks from behind/sides are not blocked.
        if (!isInBlockingArc(player, event)) {
            return;
        }
        ItemStack weapon = player.getUseItem();
        // One block costs ceil(10% of the incoming damage) sharpness.
        int loss = (int) Math.ceil(event.getAmount() * 0.10D);
        Sharpness.set(weapon, Math.max(0, Sharpness.get(weapon) - loss));
        // Cancelling the damage would otherwise skip the hurt cooldown, letting the same attacker
        // hit again on every tick and re-trigger the parry sound continuously. Grant the normal
        // short invulnerability window so one hit produces exactly one parry sound.
        player.hurtTime = 0;
        player.hurtDuration = 0;
        // Force a random 0.3-0.8 s gap between parry sound/particle bursts (slimes and other
        // fast attackers cannot retrigger them every tick).
        long now = player.level().getGameTime();
        long nextFx = player.getPersistentData().getLong("bettercombat_neonarsenal_next_parry_fx");
        if (now >= nextFx) {
            player.getPersistentData().putLong("bettercombat_neonarsenal_next_parry_fx",
                    now + 6L + player.getRandom().nextInt(11));
            player.level().playSound(null, player.blockPosition(), ModSounds.PARRY.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player.level() instanceof ServerLevel level) {
                Vec3 look = player.getLookAngle();
                double px = player.getX() + look.x * 0.7D;
                double py = player.getY() + 1.2D;
                double pz = player.getZ() + look.z * 0.7D;
                // Always emit sparks on a block; colour follows the weapon's particle colour, and the
                // glow flag follows the glow ink sac (blaze powder is not required).
                level.sendParticles(new BlockSparkOptions(Forge.displayColor(weapon), Forge.glowEnabled(weapon)),
                        px, py, pz, 14, 0.25D, 0.25D, 0.25D, 0.18D);
            }
            Vec3 look = player.getLookAngle();
            player.push(-look.x * 0.10D, 0.0D, -look.z * 0.10D);
            player.hurtMarked = true;
        }
        event.setCanceled(true);
    }

    /** {@return true when the damage source lies within the 210-degree frontal blocking arc}. */
    private static boolean isInBlockingArc(ServerPlayer player, LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getDirectEntity();
        if (source == null) {
            source = event.getSource().getEntity();
        }
        if (source == null) {
            // Environmental damage has no direction and is not blocked by the frontal guard.
            return false;
        }
        Vec3 toSource = new Vec3(source.getX() - player.getX(), 0.0D, source.getZ() - player.getZ());
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (toSource.lengthSqr() < 1.0E-6D || look.lengthSqr() < 1.0E-6D) {
            return true;
        }
        double dot = toSource.normalize().dot(look.normalize());
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))));
        return angle <= 105.0D;
    }

    /** Blocking is the highest-priority action: attacks are cancelled while blocking. */
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isBlocking(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Blocking is the highest-priority action: mining/left-clicking a block is cancelled. */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isBlocking(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Right-clicking a block with a weapon starts blocking instead of interacting with the block. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity().getMainHandItem().getItem() instanceof SharpnessWeaponItem)) {
            return;
        }
        event.setCanceled(true);
        event.getEntity().startUsingItem(InteractionHand.MAIN_HAND);
    }

    /** Tracks block duration and breaks the combo after a long block. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        boolean blocking = isBlocking(player);
        Long start = BLOCK_START.get(id);
        if (blocking) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            // Sneaking is suppressed while blocking.
            if (player.isShiftKeyDown()) {
                player.setShiftKeyDown(false);
            }
            if (start == null) {
                BLOCK_START.put(id, player.level().getGameTime());
            }
            return;
        }
        if (start != null) {
            BLOCK_START.remove(id);
            // Blocking is fully invulnerable (all incoming damage is cancelled). After the block
            // ends, keep a short 0.2 s invulnerability window.
            player.invulnerableTime = Math.max(player.invulnerableTime, 4);
            long duration = player.level().getGameTime() - start;
            if (duration > LONG_BLOCK_TICKS) {
                resetCombo(player);
                player.getPersistentData().putLong("bettercombat_neonarsenal_last_long_block",
                        player.level().getGameTime());
            }
        }
    }

    /** Resets the Better Combat combo counter; works without a compile-time BC dependency. */
    private static void resetCombo(ServerPlayer player) {
        if (!comboLookupTried) {
            comboLookupTried = true;
            try {
                Class<?> properties = Class.forName("net.bettercombat.logic.PlayerAttackProperties");
                setComboCountMethod = properties.getMethod("setComboCount", int.class);
            } catch (Throwable ignored) {
                setComboCountMethod = null;
            }
        }
        if (setComboCountMethod == null) {
            return;
        }
        try {
            setComboCountMethod.invoke(player, -1);
        } catch (Throwable ignored) {
            // Combo reset is optional if Better Combat changes its internals.
        }
    }
}
