package com.zszq.bettercombatarsenal.mixin;

import java.util.List;
import java.util.Set;

import net.neoforged.fml.ModList;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the Better Combat compatibility mixin only when Better Combat is loaded, so the mod keeps
 * working (without the jump attack) when it is absent.
 */
public final class ArsenalMixinPlugin implements IMixinConfigPlugin {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("bettercombatarsenal/mixin");

    private static Boolean betterCombatLoaded;
    private static boolean loggedDecision;

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.debug("Arsenal mixin config loaded (package {})", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean apply = isBetterCombatLoaded();
        if (!loggedDecision) {
            loggedDecision = true;
            LOGGER.debug("Arsenal mixin {} for target {} (bettercombat loaded: {})",
                    apply ? "enabled" : "skipped", targetClassName, apply);
        }
        return apply;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }

    private static boolean isBetterCombatLoaded() {
        if (betterCombatLoaded == null) {
            betterCombatLoaded = detect();
            if (betterCombatLoaded == null) {
                betterCombatLoaded = false;
            }
        }
        return betterCombatLoaded;
    }

    private static Boolean detect() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded("bettercombat");
            }
        } catch (Throwable ignored) {
            // Fall through to the class check below.
        }
        try {
            Class.forName("net.bettercombat.api.AttackHand", false,
                    Thread.currentThread().getContextClassLoader());
            return Boolean.TRUE;
        } catch (Throwable ignored) {
            return Boolean.FALSE;
        }
    }
}
