package com.zszq.neonarsenal.item;

import java.util.List;

import com.zszq.neonarsenal.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/** Forge ("锻刃") layers of an arsenal weapon and their colours. */
public final class Forge {

    public static final int MAX_LAYERS = 3;
    /** Each material can be forged at most twice: the second copy unlocks its on-hit effect. */
    public static final int MAX_LEVEL = 2;
    public static final String CHORUS = "chorus";
    public static final String CREEPER = "creeper";
    public static final String RABBIT = "rabbit";
    public static final String ECHO = "echo";
    /** Vanilla teleport/portal particle purple (PortalParticle colour at full strength: 0.9, 0.3, 1.0). */
    public static final int CHORUS_COLOR = 0xE64CFF;
    /** Creeper green. */
    public static final int CREEPER_COLOR = 0x4CFF4C;
    /** Rabbit foot tan. */
    public static final int RABBIT_COLOR = 0xD2B48C;
    /** Deep Dark / sculk teal. */
    public static final int ECHO_COLOR = 0x0E7C7B;

    private Forge() {
    }

    public static List<String> layers(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        String value = stack.get(ModDataComponents.FORGE.get());
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }

    public static boolean has(ItemStack stack, String layer) {
        return layers(stack).contains(layer);
    }

    /** {@return how many times this material has been forged onto the weapon (0, 1 or 2)}. */
    public static int level(ItemStack stack, String layer) {
        int count = 0;
        for (String current : layers(stack)) {
            if (current.equals(layer)) {
                count++;
            }
        }
        return count;
    }

    /** {@return the forged layers without duplicates, in first-forged order}. */
    public static List<String> distinctLayers(ItemStack stack) {
        return List.copyOf(new java.util.LinkedHashSet<>(layers(stack)));
    }

    /**
     * Forging one copy of a material only grants its carried ("while held") bonus; the material's
     * on-hit effect unlocks once the same material has been forged a second time.
     */
    public static boolean hitEffectActive(ItemStack stack, String layer) {
        return level(stack, layer) >= 2;
    }

    public static boolean canAdd(ItemStack stack, String layer) {
        return layers(stack).size() < MAX_LAYERS && level(stack, layer) < MAX_LEVEL;
    }

    public static void add(ItemStack stack, String layer) {
        if (!canAdd(stack, layer)) {
            return;
        }
        List<String> current = new java.util.ArrayList<>(layers(stack));
        current.add(layer);
        stack.set(ModDataComponents.FORGE.get(), String.join(",", current));
        updateRarity(stack);
    }

    /** {@return the rarity tiers granted by the current layers: one tier per two forged layers}. */
    public static int rarityTiers(ItemStack stack) {
        return layers(stack).size() / 2;
    }

    /**
     * Applies the forge rarity bonus: one rarity tier per two forged layers (layers 0-1 stay
     * COMMON, layers 2-3 give UNCOMMON, and so on up to EPIC), stored in
     * {@code DataComponents.RARITY} so the item name colour follows it. The grindstone wipe removes
     * the component again.
     */
    public static void updateRarity(ItemStack stack) {
        int tiers = rarityTiers(stack);
        if (tiers <= 0) {
            stack.remove(net.minecraft.core.component.DataComponents.RARITY);
            return;
        }
        net.minecraft.world.item.Rarity[] values = net.minecraft.world.item.Rarity.values();
        stack.set(net.minecraft.core.component.DataComponents.RARITY,
                values[Math.min(values.length - 1, tiers)]);
    }

    /**
     * Mixed colour of all forge layers: HSV hue interpolation.
     *
     * <p>All layer hues are averaged as unit vectors (shortest arc on the hue circle), saturation and
     * value are averaged arithmetically, then the result is converted back to RGB. This keeps the
     * combined colour on the hue ring instead of washing out to white/grey.</p>
     */
    public static int color(ItemStack stack) {
        List<String> layers = distinctLayers(stack);
        if (layers.isEmpty()) {
            return 0xFFFFFF;
        }
        float cos = 0.0F;
        float sin = 0.0F;
        float saturation = 0.0F;
        float value = 0.0F;
        for (String layer : layers) {
            float[] hsv = rgbToHsv(colorOf(layer));
            double radians = Math.toRadians(hsv[0]);
            cos += (float) Math.cos(radians);
            sin += (float) Math.sin(radians);
            saturation += hsv[1];
            value += hsv[2];
        }
        float hue = (float) Math.toDegrees(Math.atan2(sin, cos));
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        int count = layers.size();
        return hsvToRgb(hue, saturation / count, value / count);
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            hue = 60.0F * (((b - r) / delta) + 2.0F);
        } else {
            hue = 60.0F * (((r - g) / delta) + 4.0F);
        }
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        float saturation = max == 0.0F ? 0.0F : delta / max;
        return new float[] {hue, saturation, max};
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        int sector = ((int) Math.floor(h)) % 6;
        float f = h - (float) Math.floor(h);
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * f);
        float t = value * (1.0F - saturation * (1.0F - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }
        int ri = Math.round(r * 255.0F);
        int gi = Math.round(g * 255.0F);
        int bi = Math.round(b * 255.0F);
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Glow ink sac forge switch: blade glow + BC trail colour are enabled. */
    public static boolean glowEnabled(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Integer value = stack.get(com.zszq.neonarsenal.registry.ModDataComponents.GLOW_FORGE.get());
        return value != null && value != 0;
    }

    /** Blaze powder forge switch: spark particles are enabled. */
    public static boolean particlesEnabled(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Integer value = stack.get(com.zszq.neonarsenal.registry.ModDataComponents.PARTICLE_FORGE.get());
        return value != null && value != 0;
    }

    /** Permanent dye colour or -1 when the weapon is not dyed. */
    public static int dyeColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        Integer dye = stack.get(com.zszq.neonarsenal.registry.ModDataComponents.DYE.get());
        return dye == null ? -1 : dye & 0xFFFFFF;
    }

    /**
     * Blends a new dye colour with the current accumulated colour (50/50 per channel). Only the
     * resulting single colour is stored, so repeated dyeing never nests or grows any data.
     */
    public static int blendDye(int current, int next) {
        int r = (((current >> 16) & 0xFF) + ((next >> 16) & 0xFF)) / 2;
        int g = (((current >> 8) & 0xFF) + ((next >> 8) & 0xFF)) / 2;
        int b = ((current & 0xFF) + (next & 0xFF)) / 2;
        return (r << 16) | (g << 8) | b;
    }

    /** Colour used by glow/particles/BC trail: dye overrides the mixed forge colour. */
    public static int displayColor(ItemStack stack) {
        int dye = dyeColor(stack);
        return dye >= 0 ? dye : color(stack);
    }

    public static int colorOf(String layer) {
        return switch (layer) {
            case CHORUS -> CHORUS_COLOR;
            case CREEPER -> CREEPER_COLOR;
            case RABBIT -> RABBIT_COLOR;
            case ECHO -> ECHO_COLOR;
            default -> 0xFFFFFF;
        };
    }

    public static String nameOf(String layer) {
        return switch (layer) {
            case CHORUS -> "紫颂果";
            case CREEPER -> "苦力怕头颅";
            case RABBIT -> "兔子脚";
            case ECHO -> "回响碎片";
            default -> layer;
        };
    }

    public static boolean hasChorus(ItemStack stack) {
        return has(stack, CHORUS);
    }

    public static boolean hasCreeper(ItemStack stack) {
        return has(stack, CREEPER);
    }

    public static boolean hasRabbit(ItemStack stack) {
        return has(stack, RABBIT);
    }

    public static boolean hasEcho(ItemStack stack) {
        return has(stack, ECHO);
    }
}
