/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// An on/off switch with its label to the left; the whole row is clickable. Calls back with the new value.
public class ToggleButton extends HibiscusButton {
    private static final int SWITCH_WIDTH = 22;
    private static final int SWITCH_HEIGHT = 12;

    private final BooleanConsumer onChange;
    private boolean value;

    public ToggleButton(int x, int y, int width, Component label, boolean initial, BooleanConsumer onChange) {
        super(x, y, width, 16, label, button -> {});
        this.value = initial;
        this.onChange = onChange;
    }

    public boolean value() {
        return value;
    }

    // Sets the value without calling back.
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        value = !value;
        onChange.accept(value);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = theme();
        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), theme.color(HibiscusTheme.HOVER));
        }
        int switchX = getRight() - SWITCH_WIDTH - 2;
        int textWidth = switchX - getX() - 6;
        var label = net.minecraft.locale.Language.getInstance().getVisualOrder(font().substrByWidth(getMessage(), textWidth));
        graphics.text(font(), label, getX() + 3, getY() + (getHeight() - 8) / 2, labelColor(), false);
        theme.draw(graphics, value ? HibiscusTheme.SPRITE_TOGGLE_ON : HibiscusTheme.SPRITE_TOGGLE_OFF,
                switchX, getY() + (getHeight() - SWITCH_HEIGHT) / 2, SWITCH_WIDTH, SWITCH_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.hibiscuslib.toggle.narration", getMessage(),
                value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF));
    }
}
