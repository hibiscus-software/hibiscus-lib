/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.gui.WidgetGalleryScreen;
import software.hibiscus.hibiscuslib.config.HibiscusLibClientConfig;

// Hibiscus Lib's client commands. /hibiscuslib widgets (the widget gallery) is debug-only: registered in a development
// environment, or when debug.widgetGallery is on in hibiscuslib-client.toml.
public final class HibiscusLibClientCommands {
    // A screen to open on the next tick: a command runs from the chat screen, which closes after it.
    private static @Nullable Supplier<Screen> pending;

    private HibiscusLibClientCommands() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HibiscusLibClientCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(HibiscusLibClientCommands::onTick);
    }

    public static boolean galleryEnabled() {
        return !FMLEnvironment.isProduction() || HibiscusLibClientConfig.WIDGET_GALLERY.getAsBoolean();
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        if (!galleryEnabled()) {
            return;
        }
        event.getDispatcher().register(Commands.literal(HibiscusLib.MODID)
                .then(Commands.literal("widgets").executes(context -> {
                    openNextTick(() -> new WidgetGalleryScreen(null));
                    return 1;
                })));
    }

    // Opens a screen on the next client tick, after the chat screen (or whatever is closing) is gone.
    public static void openNextTick(Supplier<Screen> screen) {
        pending = screen;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Supplier<Screen> screen = pending;
        if (screen != null) {
            pending = null;
            Minecraft.getInstance().gui.setScreen(screen.get());
        }
    }
}
