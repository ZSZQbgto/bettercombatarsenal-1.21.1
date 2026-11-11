package com.zszq.neonarsenal.client;

import com.zszq.neonarsenal.client.fx.TrailFollow;
import com.zszq.neonarsenal.client.fx.WeaponTrailFx;
import com.zszq.neonarsenal.compat.AttackAnimationTracker;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Drops client-side render state when leaving a world, so no player/particle/model reference leaks. */
public final class ClientStateCleanup {

    private ClientStateCleanup() {
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BlockPose.clearAll();
        HoldPose.clearAll();
        FirstPersonParryPose.clearAll();
        AttackAnimationTracker.clearAll();
        TrailFollow.clear();
        WeaponTrailFx.clearCaptures();
        ThirdPersonAttackPose.clearAll();
    }
}
