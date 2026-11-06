package com.zszq.bettercombatarsenal.compat;

import com.zszq.bettercombatarsenal.BetterCombatArsenal;
import com.zszq.bettercombatarsenal.weapon.WeaponCatalog;
import com.zszq.bettercombatarsenal.weapon.WeaponDefinition;

import net.neoforged.fml.ModList;

/**
 * Optional, backwards-compatible Better Combat link.
 *
 * <p>The mod intentionally contains <b>no</b> Better Combat classes, imports or hard dependency:
 * the whole integration is data driven through
 * {@code data/bettercombatarsenal/weapon_attributes/<weapon id>.json} files. This class only
 * reports the detected integration state at startup so pack authors can diagnose a missing
 * preset quickly. Everything here is safe when Better Combat is not installed.</p>
 */
public final class BetterCombatIntegration {

    public static final String MOD_ID = "bettercombat";

    private BetterCombatIntegration() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Logs whether Better Combat is present and which preset each catalogue weapon expects.
     * Called once during common setup; never throws when Better Combat is missing.
     */
    public static void reportStatus() {
        if (isAvailable()) {
            BetterCombatArsenal.LOGGER.info(
                    "Better Combat detected; weapon attributes are read from data/{}/weapon_attributes/.",
                    BetterCombatArsenal.MODID);
        } else {
            BetterCombatArsenal.LOGGER.warn(
                    "Better Combat is not installed; arsenal weapons keep vanilla combat behaviour. "
                            + "Install Better Combat 2.x (NeoForge, MC 1.21.1) for preset attack animations.");
        }

        for (WeaponDefinition weapon : WeaponCatalog.all()) {
            if (weapon.hasBetterCombatPreset()) {
                BetterCombatArsenal.LOGGER.info("Arsenal weapon '{}' uses Better Combat preset family '{}'",
                        weapon.id(), weapon.betterCombatPreset());
            } else {
                BetterCombatArsenal.LOGGER.info("Arsenal weapon '{}' uses vanilla combat behaviour", weapon.id());
            }
        }
    }
}
