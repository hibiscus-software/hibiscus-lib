/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.compat.jei;

import java.util.Collection;
import java.util.Optional;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.button.ButtonBar;
import software.hibiscus.hibiscuslib.client.gui.BaseScreen;
import software.hibiscus.hibiscuslib.client.gui.widget.ItemSlotWidget;

// JEI integration, loaded by JEI only when it's installed: keeps JEI's item list and bookmarks clear of the sidebar
// buttons, lets JEI's keys work over Hibiscus item slots, and hands the runtime to JeiBridge for RecipeViewers.
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

            // JEI's keys (show recipes, uses, bookmark) over item slots in Hibiscus screens.
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, double mouseX, double mouseY) {
                if (Minecraft.getInstance().gui.screen() instanceof BaseScreen screen) {
                    ItemSlotWidget slot = screen.slotUnderMouse(mouseX, mouseY);
                    if (slot != null) {
                        return factory.createBuilder(slot.stack()).buildWithArea(slot.getX(), slot.getY(), slot.getWidth(), slot.getHeight());
                    }
                }
                return Optional.empty();
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
