package com.zszq.neonarsenal.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accessor so the fall-slam state can invoke Minecraft's private attack starter. */
@Mixin(Minecraft.class)
public interface MinecraftAttackAccessor {

    @Invoker("startAttack")
    boolean neonarsenal$startAttack();
}
