/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.Icon;

// A themed button with a centred label and an optional icon before it. An "accent" button (the dialog's main
// action) draws its label in the accent colour.
public class TextButton extends HibiscusButton {
    private Icon icon = Icon.NONE;
    private boolean accent;

    public TextButton(int x, int y, int width, int height, Component label, Consumer<HibiscusButton> onPress) {
        super(x, y, width, height, label, onPress);
    }

    public TextButton(int width, Component label, Consumer<HibiscusButton> onPress) {
        this(0, 0, width, 20, label, onPress);
    }

    public TextButton icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public TextButton accent() {
        this.accent = true;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractBackground(graphics);
        boolean hasIcon = icon != Icon.NONE;
        int room = getWidth() - 8 - (hasIcon ? 18 : 0);
        var label = Language.getInstance().getVisualOrder(font().substrByWidth(getMessage(), room));
        int textWidth = font().width(label);
        int contentWidth = textWidth + (hasIcon ? 18 : 0);
        int x = getX() + (getWidth() - contentWidth) / 2;
        if (hasIcon) {
            icon.draw(graphics, x, getY() + (getHeight() - 16) / 2);
            x += 18;
        }
        int color = accent && active ? theme().color(isHoveredOrFocused() ? HibiscusTheme.ACCENT_HOVER : HibiscusTheme.ACCENT) : labelColor();
        graphics.text(font(), label, x, getY() + (getHeight() - 8) / 2 + (isPressed() ? 1 : 0), color, false);
    }
}
