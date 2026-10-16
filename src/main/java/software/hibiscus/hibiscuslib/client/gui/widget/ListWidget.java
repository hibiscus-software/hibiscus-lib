/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.Icon;

/**
 * A scrolling list of selectable rows, each with an icon and two lines of text (a title and a muted subtitle). Click or
 * the arrow keys select; a double click or Enter activates. Rows come from a {@link RowRenderer} so any type can be
 * listed (players, teams, quests). With {@link #onContextMenu(ContextMenuHandler)}, a right click on a row (or Shift+F10
 * or the Menu key on the selected row) selects it and asks for its context menu.
 */
public class ListWidget<T> extends AbstractScrollArea {
    public static final int ROW_HEIGHT = 22;
    // GLFW_KEY_MENU (InputConstants has no name for it).
    private static final int KEY_MENU = 348;

    // How a value shows as a row.
    public record RowRenderer<T>(Function<T, Icon> icon, Function<T, Component> title, Function<T, @Nullable Component> subtitle) {
        public static <T> RowRenderer<T> of(Function<T, Icon> icon, Function<T, Component> title, Function<T, @Nullable Component> subtitle) {
            return new RowRenderer<>(icon, title, subtitle);
        }
    }

    // Opens a row's context menu at a screen position (usually a ContextMenu built there).
    @FunctionalInterface
    public interface ContextMenuHandler<T> {
        void open(T row, int x, int y);
    }

    private final RowRenderer<T> renderer;
    private final List<T> rows = new ArrayList<>();
    private @Nullable T selected;
    private Consumer<@Nullable T> onSelect = value -> {};
    private Consumer<T> onActivate = value -> {};
    private @Nullable ContextMenuHandler<T> onContextMenu;
    private Component emptyText = Component.empty();
    private boolean dragging;

    public ListWidget(int x, int y, int width, int height, RowRenderer<T> renderer) {
        super(x, y, width, height, Component.empty(), ThemedScrollbar.SETTINGS);
        this.renderer = renderer;
    }

    public ListWidget<T> onSelect(Consumer<@Nullable T> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    public ListWidget<T> onActivate(Consumer<T> onActivate) {
        this.onActivate = onActivate;
        return this;
    }

    // Right click on a row, or Shift+F10 / the Menu key on the selected one: selects the row and opens its menu.
    public ListWidget<T> onContextMenu(ContextMenuHandler<T> onContextMenu) {
        this.onContextMenu = onContextMenu;
        return this;
    }

    // Shown in the middle of the list while it's empty.
    public ListWidget<T> emptyText(Component emptyText) {
        this.emptyText = emptyText;
        return this;
    }

    // Replaces the rows, keeping the selection if the selected value is still listed.
    public void setRows(List<? extends T> values) {
        rows.clear();
        rows.addAll(values);
        if (selected != null && !rows.contains(selected)) {
            select(null);
        }
        refreshScrollAmount();
    }

    public List<T> rows() {
        return List.copyOf(rows);
    }

    public @Nullable T selected() {
        return selected;
    }

    public void select(@Nullable T value) {
        if (value != selected) {
            selected = value;
            onSelect.accept(value);
        }
        if (value != null) {
            scrollToRow(rows.indexOf(value));
        }
    }

    private void scrollToRow(int index) {
        if (index < 0) {
            return;
        }
        int top = index * ROW_HEIGHT;
        if (top < scrollAmount()) {
            setScrollAmount(top);
        } else if (top + ROW_HEIGHT > scrollAmount() + getHeight() - 2) {
            setScrollAmount(top + ROW_HEIGHT - getHeight() + 2);
        }
    }

    @Override
    protected int contentHeight() {
        return rows.size() * ROW_HEIGHT + 2;
    }

    @Override
    public int scrollbarWidth() {
        return ThemedScrollbar.WIDTH;
    }

    private int rowAt(double mouseY) {
        int index = (int) ((mouseY - getY() - 1 + scrollAmount()) / ROW_HEIGHT);
        return mouseY >= getY() && index >= 0 && index < rows.size() ? index : -1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        Font font = Minecraft.getInstance().font;
        theme.inset(graphics, getX(), getY(), getWidth(), getHeight());
        if (rows.isEmpty()) {
            graphics.centeredText(font, emptyText, getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, theme.textMuted());
            return;
        }
        int right = getRight() - (scrollable() ? ThemedScrollbar.WIDTH + 2 : 1);
        int hovered = isHovered() && !isOverScrollbar(mouseX, mouseY) ? rowAt(mouseY) : -1;
        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        int first = Math.max(0, (int) (scrollAmount() / ROW_HEIGHT));
        for (int i = first; i < rows.size(); i++) {
            int y = getY() + 1 + i * ROW_HEIGHT - (int) scrollAmount();
            if (y > getBottom()) {
                break;
            }
            T value = rows.get(i);
            if (value.equals(selected)) {
                graphics.fill(getX() + 1, y, right, y + ROW_HEIGHT, theme.color(HibiscusTheme.SELECTION));
                graphics.fill(getX() + 1, y, getX() + 3, y + ROW_HEIGHT, theme.accent());
            } else if (i == hovered) {
                graphics.fill(getX() + 1, y, right, y + ROW_HEIGHT, theme.color(HibiscusTheme.HOVER));
            }
            renderer.icon().apply(value).draw(graphics, getX() + 5, y + 3);
            int textX = getX() + 25;
            int room = right - textX - 2;
            Component title = renderer.title().apply(value);
            Component subtitle = renderer.subtitle().apply(value);
            int titleY = subtitle == null ? y + 7 : y + 2;
            graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(title, room)), textX, titleY, theme.text(), false);
            if (subtitle != null) {
                graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(subtitle, room)), textX, y + 12, theme.textMuted(), false);
            }
        }
        graphics.disableScissor();
        extractScrollbar(graphics, mouseX, mouseY);
        if (isFocused()) {
            graphics.outline(getX(), getY(), getWidth(), getHeight(), theme.accent());
        }
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (scrollable()) {
            boolean hot = dragging || isOverScrollbar(mouseX, mouseY);
            ThemedScrollbar.draw(graphics, scrollBarX() - 1, getY() + 1, getHeight() - 2, scrollBarY(), scrollerHeight() - 2, hot);
        }
    }

    // Right clicks count too once there's a context menu to open.
    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return super.isValidClickButton(buttonInfo)
                || onContextMenu != null && buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            int index = rowAt(event.y());
            if (onContextMenu != null && index >= 0 && !isOverScrollbar(event.x(), event.y())) {
                T value = rows.get(index);
                select(value);
                onContextMenu.open(value, (int) event.x(), (int) event.y());
            }
            return;
        }
        dragging = updateScrolling(event);
        if (dragging) {
            return;
        }
        int index = rowAt(event.y());
        if (index >= 0) {
            T value = rows.get(index);
            select(value);
            if (doubleClick) {
                onActivate.accept(value);
            }
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        dragging = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (rows.isEmpty()) {
            return false;
        }
        int index = selected == null ? -1 : rows.indexOf(selected);
        boolean menuKey = event.key() == KEY_MENU || event.key() == InputConstants.KEY_F10 && event.hasShiftDown();
        if (menuKey && onContextMenu != null && selected != null) {
            // Under the selected row, where the mouse would be.
            int rowY = getY() + 1 + index * ROW_HEIGHT - (int) scrollAmount();
            onContextMenu.open(selected, getX() + 25, Math.min(getBottom(), rowY + ROW_HEIGHT));
            return true;
        }
        switch (event.key()) {
            case InputConstants.KEY_DOWN -> select(rows.get(Math.min(rows.size() - 1, index + 1)));
            case InputConstants.KEY_UP -> select(rows.get(Math.max(0, index - 1)));
            case InputConstants.KEY_HOME -> select(rows.getFirst());
            case InputConstants.KEY_END -> select(rows.getLast());
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (selected == null) {
                    return false;
                }
                onActivate.accept(selected);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (selected != null) {
            output.add(NarratedElementType.TITLE, Component.translatable("gui.hibiscuslib.list.narration",
                    renderer.title().apply(selected), rows.indexOf(selected) + 1, rows.size()));
        }
    }
}
