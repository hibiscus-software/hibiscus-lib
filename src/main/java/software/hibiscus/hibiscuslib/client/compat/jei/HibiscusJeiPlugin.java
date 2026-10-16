/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.compat.jei;

import java.util.Collection;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.button.ButtonBar;

// JEI integration, loaded by JEI only when it's installed: keeps JEI's item list and bookmarks clear of the sidebar
// buttons, and hands the runtime to JeiBridge for RecipeViewers.
@JeiPlugin
public class HibiscusJeiPlugin implements IModPlugin {
    private static final Identifier UID = HibiscusLib.id("jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return ButtonBar.areas();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiBridge.runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiBridge.runtime = null;
    }
}
