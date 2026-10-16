/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// A themed box that holds other widgets: they draw over it, take input through it and are reached with Tab like
// any other widget. Children use screen coordinates; moving the panel moves them with it.
public class Panel extends AbstractContainerWidget {
    public enum Style {
        // A raised panel (the window look).
        PANEL,
        // A recessed area inside a panel.
        INSET,
        // Nothing drawn; only groups widgets.
        NONE
    }

    private final List<AbstractWidget> children = new ArrayList<>();
    private Style style;

    public Panel(int x, int y, int width, int height, Style style) {
        super(x, y, width, height, Component.empty());
        this.style = style;
    }

    public <T extends AbstractWidget> T add(T widget) {
        children.add(widget);
        return widget;
    }

    // Adds a widget at a position relative to the panel's top-left corner.
    public <T extends AbstractWidget> T add(T widget, int relX, int relY) {
        widget.setPosition(getX() + relX, getY() + relY);
        return add(widget);
    }

    public void remove(AbstractWidget widget) {
        children.remove(widget);
    }

    public void clear() {
        children.clear();
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public void setX(int x) {
        int dx = x - getX();
        super.setX(x);
        if (children != null) {
            children.forEach(child -> child.setX(child.getX() + dx));
        }
    }

    @Override
    public void setY(int y) {
        int dy = y - getY();
        super.setY(y);
        if (children != null) {
            children.forEach(child -> child.setY(child.getY() + dy));
        }
    }

    @Override
    protected int contentHeight() {
        return getHeight();
    }

    @Override
    protected double scrollRate() {
        return 0;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        drawBackground(graphics, style, getX(), getY(), getWidth(), getHeight());
        for (AbstractWidget child : children) {
            child.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    static void drawBackground(GuiGraphicsExtractor graphics, Style style, int x, int y, int width, int height) {
        switch (style) {
            case PANEL -> HibiscusTheme.current().panel(graphics, x, y, width, height);
            case INSET -> HibiscusTheme.current().inset(graphics, x, y, width, height);
            case NONE -> {}
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        for (AbstractWidget child : children) {
            if (child.isMouseOver(x, y) && child.mouseScrolled(x, y, scrollX, scrollY)) {
                return true;
            }
        }
        return false;
    }

    // The narrator reads the focused child.
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) {
            widget.updateNarration(output);
        }
    }
}
