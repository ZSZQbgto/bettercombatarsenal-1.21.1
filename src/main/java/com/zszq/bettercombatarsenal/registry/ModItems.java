package com.zszq.bettercombatarsenal.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zszq.bettercombatarsenal.BetterCombatArsenal;
import com.zszq.bettercombatarsenal.weapon.WeaponCatalog;
import com.zszq.bettercombatarsenal.weapon.WeaponDefinition;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers every weapon from {@link WeaponCatalog} into the item registry.
 *
 * <p>This class only translates catalogue entries into {@link DeferredItem}s; it contains no
 * per-weapon knowledge, so adding a weapon to the catalogue is enough to get it registered.</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterCombatArsenal.MODID);

    private static final Map<String, DeferredItem<Item>> WEAPONS = new LinkedHashMap<>();

    static {
        for (WeaponDefinition definition : WeaponCatalog.all()) {
            WEAPONS.put(definition.id(), ITEMS.register(definition.id(), definition::createItem));
        }
    }

    private ModItems() {
    }

    /** {@return the deferred item for the given catalogue id}. */
    public static DeferredItem<Item> get(String id) {
        DeferredItem<Item> item = WEAPONS.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown arsenal weapon id: " + id);
        }
        return item;
    }

    public static Collection<DeferredItem<Item>> all() {
        return Collections.unmodifiableCollection(WEAPONS.values());
    }

    public static List<String> ids() {
        return List.copyOf(WEAPONS.keySet());
    }
}
