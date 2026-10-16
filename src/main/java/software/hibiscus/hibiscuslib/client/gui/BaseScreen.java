/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.popup.Popup;
import software.hibiscus.hibiscuslib.client.gui.widget.ItemSlotWidget;

/**
 * Base for every Hibiscus screen: a centred panel sized to fit (so it works from GUI scale 1 to 4, down to a 320x180
 * screen), a title, a stack of modal {@link Popup}s (dialogs, context menus), tooltips, and the same close behaviour
 * everywhere: Esc closes the top popup, else the screen; a right click outside the panel closes the screen, back to
 * the screen it was opened from.
 *
 * <p>Subclasses add their widgets in {@link #initContent()} using {@link #panelX}, {@link #panelY},
 * {@link #panelWidth} and {@link #panelHeight}, and usually only need {@link #preferredWidth()} and
 * {@link #preferredHeight()} besides.
 */
public abstract class BaseScreen extends Screen {
    // Room kept round the panel at the screen's edges.
    protected static final int MARGIN = 8;
    // Where content starts below the panel's top edge (under the title).
    protected static final int HEADER = 22;

    protected final @Nullable Screen parent;
    protected int panelX;
    protected int panelY;
    protected int panelWidth;
    protected int panelHeight;

    private final Deque<Popup> popups = new ArrayDeque<>();

    protected BaseScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    // The panel size the screen would like; it shrinks to fit smaller screens.
    protected abstract int preferredWidth();

    protected abstract int preferredHeight();

    // Adds the screen's widgets; the panel bounds are set by then.
    protected abstract void initContent();

    @Override
    protected final void init() {
        panelWidth = Math.min(preferredWidth(), width - MARGIN * 2);
        panelHeight = Math.min(preferredHeight(), height - MARGIN * 2);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        initContent();
        // Popups survive a resize, re-placed for the new size.
        for (Popup popup : popups) {
            popup.position(width, height);
            popup.attach(this);
        }
    }

    // ---- Popups

    public void openPopup(Popup popup) {
        popups.push(popup);
        popup.position(width, height);
        popup.attach(this);
        setFocused(popup);
    }

    public void closePopup(Popup popup) {
        popups.remove(popup);
        setFocused(popups.peek());
    }

    public Optional<Popup> topPopup() {
        return Optional.ofNullable(popups.peek());
    }

    // While a popup is open, it alone gets input (and Tab cycles within it).
    @Override
    public List<? extends GuiEventListener> children() {
        Popup top = popups.peek();
        return top != null ? List.of(top) : super.children();
    }

    // ---- Drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean covered = !popups.isEmpty();
        int screenMouseX = covered ? -1 : mouseX;
        int screenMouseY = covered ? -1 : mouseY;
        extractPanel(graphics, screenMouseX, screenMouseY, a);
        super.extractRenderState(graphics, screenMouseX, screenMouseY, a);
        extractForeground(graphics, screenMouseX, screenMouseY, a);
        HibiscusTheme theme = HibiscusTheme.current();
        Popup top = popups.peek();
        for (Popup popup : popups.reversed()) {
            graphics.nextStratum();
            if (popup.dimsBackground()) {
                graphics.fill(0, 0, width, height, theme.color(HibiscusTheme.OVERLAY));
            }
            boolean live = popup == top;
            popup.extractRenderState(graphics, live ? mouseX : -1, live ? mouseY : -1, a);
        }
    }

    // The panel and title, under the widgets.
    protected void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        theme.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        graphics.text(font, title, panelX + 8, panelY + 7, theme.text(), false);
    }

    // Anything drawn over the widgets but under popups.
    protected void extractForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {}

    // ---- Input and closing

    @Override
    public boolean keyPressed(KeyEvent event) {
        Popup top = popups.peek();
        if (top != null && event.isEscape()) {
            top.cancel();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Popup top = popups.peek();
        if (top != null && !top.isMouseOver(event.x(), event.y())) {
            return top.clickedOutside(event);
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (top == null && event.button() == InputConstants.MOUSE_BUTTON_RIGHT && closesOnRightClickOutside() && !isOverPanel(event.x(), event.y())) {
            onClose();
            return true;
        }
        return false;
    }

    protected boolean closesOnRightClickOutside() {
        return true;
    }

    protected boolean isOverPanel(double x, double y) {
        return x >= panelX && x < panelX + panelWidth && y >= panelY && y < panelY + panelHeight;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Runs a text click (a link in a TextLayout, a command in a quest description) as chat would, asking before
    // opening links.
    public void handleClickEvent(ClickEvent event) {
        if (minecraft.player != null) {
            defaultHandleGameClickEvent(event, minecraft, this);
        } else {
            defaultHandleClickEvent(event, minecraft, this);
        }
    }

    // The item slot under the mouse, for recipe viewers' keys (R, U, A in JEI) over Hibiscus item slots.
    public @Nullable ItemSlotWidget slotUnderMouse(double x, double y) {
        return findSlot(children(), x, y);
    }

    private static @Nullable ItemSlotWidget findSlot(List<? extends GuiEventListener> listeners, double x, double y) {
        for (GuiEventListener listener : listeners) {
            if (!listener.isMouseOver(x, y)) {
                continue;
            }
            if (listener instanceof ItemSlotWidget slot && slot.visible && !slot.stack().isEmpty()) {
                return slot;
            }
            if (listener instanceof ContainerEventHandler container && (!(listener instanceof AbstractWidget widget) || widget.visible)) {
                ItemSlotWidget slot = findSlot(container.children(), x, y);
                if (slot != null) {
                    return slot;
                }
            }
        }
        return null;
    }
}
