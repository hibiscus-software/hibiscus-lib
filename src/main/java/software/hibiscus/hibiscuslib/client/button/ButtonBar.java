/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.button;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import software.hibiscus.hibiscuslib.button.ButtonLayout;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;
import software.hibiscus.hibiscuslib.config.HibiscusLibClientConfig;

// Puts the sidebar buttons on the survival and creative inventories. The widgets are added when the screen initialises
// and laid out again every frame from the screen's leftPos/topPos, so they follow the recipe book as it opens and
// closes (which moves the panel without re-initialising the screen).
public final class ButtonBar {
    // How far the recipe book's category tabs stick out left of the book.
    private static final int RECIPE_BOOK_TABS = 30;
    // JEI draws its bookmark buttons in the bottom corners whatever the exclusion areas say, so a tall column stops short.
    private static final int JEI_BOTTOM_BUTTONS = 24;
    private static final boolean JEI = ModList.get().isLoaded("jei");

    // The open inventory's buttons; empty when none is open.
    private static List<SidebarButton> current = List.of();
    private static Screen currentScreen;

    private ButtonBar() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ButtonBar::onInit);
        NeoForge.EVENT_BUS.addListener(ButtonBar::onRender);
        NeoForge.EVENT_BUS.addListener(ButtonBar::onClosing);
    }

    public static boolean appliesTo(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    private static void onInit(ScreenEvent.Init.Post event) {
        current = List.of();
        currentScreen = null;
        if (!appliesTo(event.getScreen()) || !HibiscusLibClientConfig.BUTTONS_ENABLED.getAsBoolean()) {
            return;
        }
        Map<Identifier, ButtonSpec> buttons = HibiscusButtons.all();
        List<SidebarButton> widgets = new ArrayList<>();
        for (Identifier id : HibiscusButtons.arrange(buttons, HibiscusLibClientConfig.order(), HibiscusLibClientConfig.hidden())) {
            SidebarButton button = new SidebarButton(id, buttons.get(id), 0, 0);
            widgets.add(button);
            event.addListener(button);
        }
        current = List.copyOf(widgets);
        currentScreen = event.getScreen();
        layout((AbstractContainerScreen<?>) event.getScreen());
    }

    private static void onRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() == currentScreen && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            layout(screen);
        }
    }

    private static void onClosing(ScreenEvent.Closing event) {
        if (event.getScreen() == currentScreen) {
            current = List.of();
            currentScreen = null;
        }
    }

    // Places the visible buttons for the configured location; buttons whose predicate says no are hidden and leave no gap.
    static void layout(AbstractContainerScreen<?> screen) {
        List<SidebarButton> shown = new ArrayList<>();
        for (SidebarButton button : current) {
            boolean visible;
            try {
                visible = button.spec().visible().getAsBoolean();
            } catch (RuntimeException e) {
                visible = false;
            }
            button.visible = visible;
            button.active = visible;
            if (visible) {
                shown.add(button);
            }
        }
        if (shown.isEmpty()) {
            return;
        }
        List<int[]> positions = ButtonLayout.place(shown.size(), HibiscusLibClientConfig.BUTTONS_LOCATION.get(), area(screen));
        for (int i = 0; i < shown.size(); i++) {
            SidebarButton button = shown.get(i);
            int[] position = positions.get(i);
            if (position == null) {
                // No room on this screen: left out rather than drawn off screen or over the panel.
                button.visible = false;
                button.active = false;
            } else {
                button.setPosition(position[0], position[1]);
            }
        }
    }

    // The panel the bar sits beside: the inventory, widened left over the recipe book (and its tabs) when that's
    // open beside it.
    private static ButtonLayout.Area area(AbstractContainerScreen<?> screen) {
        int left = screen.getLeftPos();
        if (screen instanceof AbstractRecipeBookScreen<?> && recipeBookBeside(screen)) {
            left -= RecipeBookComponent.IMAGE_WIDTH + 1 + RECIPE_BOOK_TABS;
        }
        return new ButtonLayout.Area(left, screen.getTopPos(), screen.getLeftPos() + screen.getImageWidth(), screen.getImageHeight(),
                screen.width, screen.height - (JEI ? JEI_BOTTOM_BUTTONS : 0));
    }

    // The recipe book pushes the panel right of centre when it opens beside it (it overlays the panel on narrow
    // screens, which leaves the panel centred).
    private static boolean recipeBookBeside(AbstractContainerScreen<?> screen) {
        return screen.getLeftPos() > (screen.width - screen.getImageWidth()) / 2 + 8;
    }

    // The screen areas the bar takes, for recipe viewers (JEI) to keep clear of.
    public static List<Rect2i> areas() {
        List<Rect2i> areas = new ArrayList<>();
        for (SidebarButton button : current) {
            if (button.visible) {
                areas.add(new Rect2i(button.getX() - 1, button.getY() - 1, SidebarButton.SIZE + 2, SidebarButton.SIZE + 2));
            }
        }
        return areas;
    }

}
