package com.zszq.neonarsenal.client.config;

import com.zszq.neonarsenal.client.config.NeonArsenalConfigScreen;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only bootstrap: registers the custom mod-list config screen. Loaded reflectively by the mod. */
public final class ClientConfigBootstrap {

    private ClientConfigBootstrap() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new NeonArsenalConfigScreen(parent));
    }
}
