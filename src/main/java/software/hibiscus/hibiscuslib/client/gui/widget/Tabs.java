/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.Icon;

// A row of tabs along the top of a panel. Click a tab, or focus the row and use Left/Right, to switch; the selected
// tab joins the panel under it. Tabs share the row's width, each at least wide enough for its icon.
public class Tabs extends HibiscusWidget {
    public static final int HEIGHT = 20;

    public record Tab(Component label, Icon icon) {
        public static Tab of(Component label) {
            return new Tab(label, Icon.NONE);
        }
    }

    private final List<Tab> tabs = new ArrayList<>();
    private final IntConsumer onSelect;
    private int selected;

    public Tabs(int x, int y, int width, List<Tab> tabs, IntConsumer onSelect) {
        super(x, y, width, HEIGHT, Component.empty());
        this.tabs.addAll(tabs);
        this.onSelect = onSelect;
    }

    public int selected() {
        return selected;
    }

    // Selects a tab without calling back (setting up a screen).
    public Tabs setSelected(int index) {
        if (index >= 0 && index < tabs.size()) {
            selected = index;
        }
        return this;
    }

    public void select(int index) {
        if (index >= 0 && index < tabs.size() && index != selected) {
            selected = index;
            onSelect.accept(index);
        }
    }

    private int tabWidth() {
        return tabs.isEmpty() ? 0 : getWidth() / tabs.size();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = theme();
        int tabWidth = tabWidth();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int x = getX() + i * tabWidth;
            int width = i == tabs.size() - 1 ? getRight() - x : tabWidth;
            boolean isSelected = i == selected;
            boolean hovered = isHovered() && mouseX >= x && mouseX < x + width;
            int top = isSelected ? getY() : getY() + 2;
            theme.draw(graphics, isSelected ? HibiscusTheme.SPRITE_TAB_SELECTED : HibiscusTheme.SPRITE_TAB, x, top, width, getBottom() - top);
            boolean hasIcon = tab.icon() != Icon.NONE;
            int room = width - 8 - (hasIcon ? 18 : 0);
            var label = Language.getInstance().getVisualOrder(font().substrByWidth(tab.label(), Math.max(0, room)));
            int contentWidth = font().width(label) + (hasIcon ? 18 : 0);
            int cx = x + (width - contentWidth) / 2;
            if (hasIcon) {
                tab.icon().draw(graphics, cx, top + (getBottom() - top - 16) / 2);
                cx += 18;
            }
            int color = isSelected ? theme.text() : hovered ? theme.color(HibiscusTheme.ACCENT_HOVER) : theme.textMuted();
            graphics.text(font(), label, cx, top + (getBottom() - top - 8) / 2 + 1, color, false);
            if (isFocused() && isSelected) {
                graphics.fill(x + 3, getBottom() - 2, x + width - 3, getBottom() - 1, theme.accent());
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int tabWidth = tabWidth();
        if (tabWidth > 0) {
            select(Math.min(tabs.size() - 1, (int) ((event.x() - getX()) / tabWidth)));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_LEFT) {
            select(selected - 1);
            return true;
        }
        if (event.key() == InputConstants.KEY_RIGHT) {
            select(selected + 1);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (!tabs.isEmpty()) {
            output.add(NarratedElementType.TITLE, Component.translatable("gui.hibiscuslib.tabs.narration",
                    tabs.get(selected).label(), selected + 1, tabs.size()));
        }
    }
}
