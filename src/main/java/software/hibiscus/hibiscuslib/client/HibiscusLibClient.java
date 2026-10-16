/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.button.ButtonBar;
import software.hibiscus.hibiscuslib.client.button.ButtonKeys;
import software.hibiscus.hibiscuslib.client.button.DataDrivenButtons;
import software.hibiscus.hibiscuslib.client.gui.ThemeLoader;
import software.hibiscus.hibiscuslib.config.HibiscusLibClientConfig;

// Client-only entrypoint; this class is never loaded on dedicated servers.
@Mod(value = HibiscusLib.MODID, dist = Dist.CLIENT)
public class HibiscusLibClient {
    public HibiscusLibClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HibiscusLibClientConfig.SPEC, HibiscusLib.MODID + "-client.toml");
        // Mods screen > Hibiscus Lib > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(HibiscusLibClient::addReloadListeners);
        ButtonBar.register();
        ButtonKeys.register(modEventBus);
    }

    private static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ThemeLoader.ID, new ThemeLoader());
        event.addListener(DataDrivenButtons.ID, new DataDrivenButtons());
    }
}
