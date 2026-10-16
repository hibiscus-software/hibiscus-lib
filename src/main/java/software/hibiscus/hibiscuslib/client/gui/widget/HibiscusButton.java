/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// Base for the themed buttons: draws the theme's button background for its state (normal, hover/focus, pressed,
// disabled), and runs its action on click, Enter or Space.
public abstract class HibiscusButton extends AbstractButton {
    private final Consumer<HibiscusButton> onPress;

    protected HibiscusButton(int x, int y, int width, int height, Component message, Consumer<HibiscusButton> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @SuppressWarnings("unchecked")
    public <T extends HibiscusButton> T withTooltip(Component tooltip) {
        setTooltip(Tooltip.create(tooltip));
        return (T) this;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.accept(this);
    }

    protected static HibiscusTheme theme() {
        return HibiscusTheme.current();
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    // Held down: hovered while the left mouse button is down.
    protected boolean isPressed() {
        return isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
    }

    protected void extractBackground(GuiGraphicsExtractor graphics) {
        theme().button(graphics, getX(), getY(), getWidth(), getHeight(), active, isHoveredOrFocused(), isPressed());
    }

    protected int labelColor() {
        return active ? theme().text() : theme().color(HibiscusTheme.TEXT_DISABLED);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
