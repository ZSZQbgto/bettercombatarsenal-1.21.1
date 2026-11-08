package com.zszq.neonarsenal.registry;

import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events used by the arsenal. */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, NeonArsenal.MODID);

    /** Parry/block success sound supplied in {@code resourcepacks/sound/parry.mp3}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = SOUND_EVENTS.register("parry",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "parry")));

    private ModSounds() {
    }
}
