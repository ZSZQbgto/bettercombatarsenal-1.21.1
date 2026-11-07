package com.zszq.neonarsenal.item;

import com.zszq.neonarsenal.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * Cooldown state bound to a specific weapon stack (not to the player). End times are absolute game
 * ticks stored in the weapon data component, so dropping, switching or moving the weapon never
 * resets a cooldown.
 */
public final class WeaponCooldowns {

    private WeaponCooldowns() {
    }

    public static boolean ready(ItemStack weapon, String id, long gameTime) {
        return end(weapon, id) <= gameTime;
    }

    public static long end(ItemStack weapon, String id) {
        for (String entry : entries(weapon)) {
            int split = entry.indexOf('=');
            if (split <= 0) {
                continue;
            }
            if (entry.substring(0, split).equals(id)) {
                try {
                    return Long.parseLong(entry.substring(split + 1));
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    public static void start(ItemStack weapon, String id, long endTick) {
        StringBuilder builder = new StringBuilder();
        for (String entry : entries(weapon)) {
            int split = entry.indexOf('=');
            if (split > 0 && entry.substring(0, split).equals(id)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(entry);
        }
        if (!builder.isEmpty()) {
            builder.append(',');
        }
        builder.append(id).append('=').append(endTick);
        weapon.set(ModDataComponents.COOLDOWNS.get(), builder.toString());
    }

    private static String[] entries(ItemStack weapon) {
        String value = weapon.get(ModDataComponents.COOLDOWNS.get());
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return value.split(",");
    }
}
