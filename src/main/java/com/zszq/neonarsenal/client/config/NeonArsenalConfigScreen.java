package com.zszq.neonarsenal.client.config;

import com.zszq.neonarsenal.config.NeonArsenalConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Config screen shown by the mod-list "Config" button: two sliders for particle density and blade
 * glow intensity (0.0 - 3.0, default 1.0). Values are saved when the screen is closed.
 */
public final class NeonArsenalConfigScreen extends Screen {

    private static final double MAX_PARTICLES = 3.0D;
    private static final double MAX_GLOW = 1.0D;

    private final Screen parent;

    public NeonArsenalConfigScreen(Screen parent) {
        super(Component.translatable("bettercombat_neonarsenal.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 110;
        int top = 60;
        this.addRenderableWidget(new Slider(left, top, 220, 20,
                Component.translatable("bettercombat_neonarsenal.configuration.particle_density"),
                NeonArsenalConfig.particleDensity() / MAX_PARTICLES, MAX_PARTICLES, NeonArsenalConfig::setParticleDensity));
        this.addRenderableWidget(new Slider(left, top + 34, 220, 20,
                Component.translatable("bettercombat_neonarsenal.configuration.blade_glow_intensity"),
                NeonArsenalConfig.bladeGlowIntensity() / MAX_GLOW, MAX_GLOW, NeonArsenalConfig::setBladeGlowIntensity));
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("bettercombat_neonarsenal.config.hint"),
                this.width / 2, this.height - 64, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        NeonArsenalConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private static final class Slider extends AbstractSliderButton {

        private final Component label;
        private final double max;
        private final java.util.function.DoubleConsumer setter;

        private Slider(int x, int y, int width, int height, Component label, double normalized, double max,
                       java.util.function.DoubleConsumer setter) {
            super(x, y, width, height, label, normalized);
            this.label = label;
            this.max = max;
            this.setter = setter;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(
                    this.label.getString() + ": " + String.format(java.util.Locale.ROOT, "%.1f", this.value * this.max)));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.value * this.max);
        }
    }
}
