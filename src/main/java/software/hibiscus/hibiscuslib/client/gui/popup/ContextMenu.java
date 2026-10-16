/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.popup;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.Icon;
import software.hibiscus.hibiscuslib.client.gui.widget.HibiscusButton;

/**
 * A right-click menu: a column of actions opened at the mouse, kept on screen. Choosing an action, clicking anywhere
 * outside or pressing Esc closes it; a click outside is used up, so it doesn't also land on whatever is underneath.
 * Build one with {@link #builder()}.
 */
public class ContextMenu extends Popup {
    private static final int ITEM_HEIGHT = 16;
    private static final int SEPARATOR_HEIGHT = 5;

    private record Item(Component label, Icon icon, Runnable action, boolean enabled, boolean danger, boolean separator) {}

    private final List<Item> items;
    private final int anchorX;
    private final int anchorY;
    // Labels line up after the icon column when any item has an icon.
    private final int textIndent;

    private ContextMenu(List<Item> items, int anchorX, int anchorY, int width, int height, int textIndent) {
        super(width, height);
        this.items = items;
        this.textIndent = textIndent;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean dimsBackground() {
        return false;
    }

    @Override
    public boolean clickedOutside(MouseButtonEvent event) {
        cancel();
        return true;
    }

    @Override
    public void position(int screenWidth, int screenHeight) {
        setPosition(Mth.clamp(anchorX, 2, Math.max(2, screenWidth - getWidth() - 2)),
                Mth.clamp(anchorY, 2, Math.max(2, screenHeight - getHeight() - 2)));
    }

    @Override
    protected void init() {
        int y = 2;
        for (Item item : items) {
            if (item.separator()) {
                y += SEPARATOR_HEIGHT;
                continue;
            }
            Entry entry = add(new Entry(item), 2, y);
            entry.active = item.enabled();
            y += ITEM_HEIGHT;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        HibiscusTheme theme = HibiscusTheme.current();
        int y = getY() + 2;
        for (Item item : items) {
            if (item.separator()) {
                graphics.fill(getX() + 4, y + 2, getRight() - 4, y + 3, theme.color(HibiscusTheme.PANEL_SHADOW));
                y += SEPARATOR_HEIGHT;
            } else {
                y += ITEM_HEIGHT;
            }
        }
    }

    private class Entry extends HibiscusButton {
        private final Item item;

        Entry(Item item) {
            super(0, 0, ContextMenu.this.getWidth() - 4, ITEM_HEIGHT, item.label(), button -> {});
            this.item = item;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            close();
            item.action().run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            HibiscusTheme theme = theme();
            Font font = font();
            if (active && isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), theme.color(HibiscusTheme.SELECTION));
            }
            if (item.icon() != Icon.NONE) {
                item.icon().draw(graphics, getX() + 2, getY());
            }
            int color = !active ? theme.color(HibiscusTheme.TEXT_DISABLED)
                    : item.danger() ? theme.color(HibiscusTheme.NEGATIVE) : theme.text();
            int textX = getX() + textIndent;
            graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(label(), getRight() - textX - 3)),
                    textX, getY() + 4, color, false);
        }
    }

    public static final class Builder {
        private final List<Item> items = new ArrayList<>();

        private Builder() {}

        public Builder add(Component label, Runnable action) {
            return add(label, Icon.NONE, action);
        }

        public Builder add(Component label, Icon icon, Runnable action) {
            items.add(new Item(label, icon, action, true, false, false));
            return this;
        }

        // Shown greyed out, e.g. an action the player's rank doesn't allow.
        public Builder addDisabled(Component label, Icon icon) {
            items.add(new Item(label, icon, () -> {}, false, false, false));
            return this;
        }

        // Drawn in the negative colour (kick, delete).
        public Builder addDanger(Component label, Icon icon, Runnable action) {
            items.add(new Item(label, icon, action, true, true, false));
            return this;
        }

        public Builder separator() {
            items.add(new Item(Component.empty(), Icon.NONE, () -> {}, false, false, true));
            return this;
        }

        // Opens at the given screen position (usually the mouse).
        public ContextMenu build(int x, int y) {
            Font font = Minecraft.getInstance().font;
            int width = 80;
            int height = 4;
            int indent = items.stream().anyMatch(item -> item.icon() != Icon.NONE) ? 21 : 5;
            for (Item item : items) {
                if (item.separator()) {
                    height += SEPARATOR_HEIGHT;
                } else {
                    width = Math.max(width, font.width(item.label()) + indent + 12);
                    height += ITEM_HEIGHT;
                }
            }
            return new ContextMenu(List.copyOf(items), x, y, width, height, indent);
        }
    }
}
