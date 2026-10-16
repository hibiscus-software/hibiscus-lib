/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

// A panel whose content is taller than it is: the mouse wheel and a draggable scrollbar move the content, which is
// clipped to the panel. Children are placed in content coordinates (relative to the content's top-left); Tab
// scrolls the focused child into view.
public class ScrollPanel extends AbstractContainerWidget {
    private static final int PADDING = 4;

    private final List<AbstractWidget> children = new ArrayList<>();
    private final Map<AbstractWidget, int[]> offsets = new IdentityHashMap<>();
    private Panel.Style style;
    private boolean dragOver;

    public ScrollPanel(int x, int y, int width, int height, Panel.Style style) {
        super(x, y, width, height, Component.empty(), ThemedScrollbar.SETTINGS);
        this.style = style;
    }

    // Adds a widget at a position in the content, relative to its top-left corner (inside the padding).
    public <T extends AbstractWidget> T add(T widget, int relX, int relY) {
        children.add(widget);
        offsets.put(widget, new int[] {relX, relY});
        place(widget);
        return widget;
    }

    public void clear() {
        children.clear();
        offsets.clear();
        setScrollAmount(0);
    }

    public void setStyle(Panel.Style style) {
        this.style = style;
    }

    // The width children have to work with, inside the padding and clear of the scrollbar.
    public int contentWidth() {
        return getWidth() - PADDING * 2 - ThemedScrollbar.WIDTH - 2;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    protected int contentHeight() {
        int bottom = 0;
        for (AbstractWidget child : children) {
            bottom = Math.max(bottom, offsets.get(child)[1] + child.getHeight());
        }
        return bottom + PADDING * 2;
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        layout();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layout();
    }

    private void layout() {
        if (children != null) {
            children.forEach(this::place);
        }
    }

    private void place(AbstractWidget child) {
        int[] offset = offsets.get(child);
        child.setPosition(getX() + PADDING + offset[0], getY() + PADDING + offset[1] - (int) scrollAmount());
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof AbstractWidget widget && offsets.containsKey(widget)) {
            int top = offsets.get(widget)[1];
            int bottom = top + widget.getHeight() + PADDING * 2;
            if (top < scrollAmount()) {
                setScrollAmount(top);
            } else if (bottom > scrollAmount() + getHeight()) {
                setScrollAmount(bottom - getHeight());
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Panel.drawBackground(graphics, style, getX(), getY(), getWidth(), getHeight());
        layout();
        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        boolean inside = isMouseOver(mouseX, mouseY);
        for (AbstractWidget child : children) {
            if (child.getBottom() >= getY() && child.getY() <= getBottom()) {
                // Outside the panel nothing underneath is hovered, even where a child pokes out of the clip.
                child.extractRenderState(graphics, inside ? mouseX : -1, inside ? mouseY : -1, a);
            }
        }
        graphics.disableScissor();
        extractScrollbar(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (scrollable()) {
            boolean hot = dragOver || isOverScrollbar(mouseX, mouseY);
            ThemedScrollbar.draw(graphics, scrollBarX() - 1, getY() + 1, getHeight() - 2, scrollBarY(), scrollerHeight() - 2, hot);
        }
    }

    @Override
    public int scrollbarWidth() {
        return ThemedScrollbar.WIDTH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        dragOver = updateScrolling(event);
        if (dragOver) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        dragOver = false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!isMouseOver(x, y)) {
            return false;
        }
        for (AbstractWidget child : children) {
            if (child.isMouseOver(x, y) && child.mouseScrolled(x, y, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) {
            widget.updateNarration(output);
        }
    }
}
