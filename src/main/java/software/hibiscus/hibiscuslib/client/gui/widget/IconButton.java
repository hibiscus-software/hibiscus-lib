/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.Icon;

// A square button showing only an icon; its label is the tooltip and what the narrator reads. "Flat" leaves out
// the background until it's hovered, for toolbars.
public class IconButton extends HibiscusButton {
    private final Icon icon;
    private boolean flat;

    public IconButton(int x, int y, int size, Icon icon, Component label, Consumer<HibiscusButton> onPress) {
        super(x, y, size, size, label, onPress);
        this.icon = icon;
        setTooltip(Tooltip.create(label));
    }

    public IconButton(Icon icon, Component label, Consumer<HibiscusButton> onPress) {
        this(0, 0, 20, icon, label, onPress);
    }

    public IconButton flat() {
        this.flat = true;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!flat || isHoveredOrFocused() || !active) {
            extractBackground(graphics);
        }
        int offset = isPressed() ? 1 : 0;
        icon.draw(graphics, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2 + offset);
        if (!active) {
            graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, 0x80202124);
        }
    }
}
