package com.zszq.neonarsenal.registry;

import com.mojang.serialization.Codec;
import com.zszq.neonarsenal.NeonArsenal;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components used by the arsenal weapons. */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NeonArsenal.MODID);

    /** Remaining sharpness of an arsenal weapon (0..3000). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHARPNESS =
            DATA_COMPONENTS.register("sharpness", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Forge layers, comma separated (max 3). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> FORGE =
            DATA_COMPONENTS.register("forge", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** Per-weapon cooldown end times in game ticks: "id=endTick,id=endTick". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> COOLDOWNS =
            DATA_COMPONENTS.register("cooldowns", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** Permanent dye colour (0xRRGGBB) forged onto a weapon; the latest dye replaces older ones. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DYE =
            DATA_COMPONENTS.register("dye", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Glow-ink-sac forge switch: 1 = blade glow + BC trail colour enabled. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GLOW_FORGE =
            DATA_COMPONENTS.register("glow_forge", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Blaze-powder forge switch: 1 = spark particles enabled. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PARTICLE_FORGE =
            DATA_COMPONENTS.register("particle_forge", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }
}
