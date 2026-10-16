/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.popup;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.input.MouseButtonEvent;
import software.hibiscus.hibiscuslib.client.gui.BaseScreen;
import software.hibiscus.hibiscuslib.client.gui.widget.Panel;

/**
 * A modal layer over a {@link BaseScreen}: while it's open it alone takes the mouse and keyboard (Tab cycles inside
 * it) and the screen under it doesn't react to hovering. Open one with {@link BaseScreen#openPopup(Popup)}.
 *
 * <p>Esc closes the top popup. A click outside it calls {@link #clickedOutside(MouseButtonEvent)}: by default a
 * right click closes it (cancels) and a left click is ignored; context menus close on any click.
 */
public abstract class Popup extends Panel {
    private @Nullable BaseScreen screen;

    protected Popup(int width, int height) {
        super(0, 0, width, height, Style.PANEL);
    }

    // Called when the popup opens, after it's been centred (or placed) on the screen: add the widgets here.
    protected abstract void init();

    public final void attach(BaseScreen screen) {
        this.screen = screen;
        clear();
        init();
    }

    public @Nullable BaseScreen screen() {
        return screen;
    }

    // Whether the screen under it is dimmed (dialogs are; menus aren't).
    public boolean dimsBackground() {
        return true;
    }

    public void close() {
        if (screen != null) {
            screen.closePopup(this);
        }
    }

    // Called when Esc closes it.
    protected void onCancel() {}

    // A click outside: returns whether it's used up. Right click cancels by default.
    public boolean clickedOutside(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            cancel();
        }
        return true;
    }

    public final void cancel() {
        onCancel();
        close();
    }

    // Placed in the middle of the screen unless a subclass says otherwise (context menus sit at the mouse).
    public void position(int screenWidth, int screenHeight) {
        setPosition((screenWidth - getWidth()) / 2, (screenHeight - getHeight()) / 2);
    }
}
