/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.io.Reader;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import software.hibiscus.hibiscuslib.HibiscusLib;

// Reloads the theme with the client's resources. Every pack's default.json is applied in pack order over the built-in
// theme, so a pack only needs the keys it changes.
public final class ThemeLoader extends SimplePreparableReloadListener<HibiscusTheme> {
    public static final Identifier ID = HibiscusLib.id("theme");
    private static final Identifier FILE = HibiscusLib.id("hibiscus_theme/default.json");

    @Override
    protected HibiscusTheme prepare(ResourceManager manager, ProfilerFiller profiler) {
        HibiscusTheme theme = HibiscusTheme.builtIn();
        List<Resource> stack = manager.getResourceStack(FILE);
        for (Resource resource : stack) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                theme = HibiscusTheme.parse(json, theme);
            } catch (Exception e) {
                HibiscusLib.LOGGER.error("Couldn't read the Hibiscus theme from pack {}", resource.sourcePackId(), e);
            }
        }
        return theme;
    }

    @Override
    protected void apply(HibiscusTheme theme, ResourceManager manager, ProfilerFiller profiler) {
        HibiscusTheme.set(theme);
    }
}
