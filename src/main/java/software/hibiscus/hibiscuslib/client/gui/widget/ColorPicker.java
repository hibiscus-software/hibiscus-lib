/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.Locale;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// Picks an opaque RGB colour (team colours, quest chapter colours): 16 preset swatches in two rows, and a hex field
// for anything else, with a preview of the current colour. Calls back with 0xRRGGBB whenever it changes.
public class ColorPicker extends Panel {
    public static final int[] PRESETS = {
            0xF2557A, 0xE5484D, 0xF28B30, 0xE8B04B, 0xF2C14E, 0x8BD346, 0x4FC78A, 0x2EC4B6,
            0x3AB7E8, 0x4C7BF2, 0x6C5CE7, 0xA363D9, 0xF07AC0, 0x9C6B4E, 0x9A9DA3, 0xE6E6E6
    };
    private static final int SWATCH = 14;
    public static final int WIDTH = SWATCH * 8 + 4 + 28;
    public static final int HEIGHT = SWATCH * 2 + 4 + 18;

    private final IntConsumer onChange;
    private final TextInput hex;
    private int color;

    public ColorPicker(int x, int y, int initial, IntConsumer onChange) {
        super(x, y, WIDTH, HEIGHT, Style.NONE);
        this.color = initial & 0xFFFFFF;
        this.onChange = onChange;
        for (int i = 0; i < PRESETS.length; i++) {
            add(new Swatch(PRESETS[i]), (i % 8) * SWATCH, (i / 8) * SWATCH);
        }
        hex = add(new TextInput(0, 0, SWATCH * 8, 18, Component.translatable("gui.hibiscuslib.color_picker.hex")), 0, SWATCH * 2 + 4);
        hex.setMaxLength(7);
        hex.setValue(format(color));
        hex.validator(text -> parse(text) >= 0);
        hex.setResponder(text -> {
            int parsed = parse(text);
            if (parsed >= 0 && parsed != color) {
                color = parsed;
                onChange.accept(color);
            }
        });
    }

    public int color() {
        return color;
    }

    // Sets the colour without calling back.
    public void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        hex.setValue(format(color));
    }

    private void pick(int rgb) {
        if (rgb != color) {
            setColor(rgb);
            onChange.accept(color);
        }
    }

    static String format(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    // #RRGGBB or RRGGBB to 0xRRGGBB, or -1.
    static int parse(String text) {
        String value = text.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        HibiscusTheme theme = HibiscusTheme.current();
        int px = getX() + SWATCH * 8 + 4;
        theme.inset(graphics, px, getY(), 28, getHeight());
        graphics.fill(px + 2, getY() + 2, px + 26, getBottom() - 2, 0xFF000000 | color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() == hex) {
            hex.updateNarration(output);
        } else {
            output.add(NarratedElementType.TITLE, Component.translatable("gui.hibiscuslib.color_picker.narration", format(color)));
        }
    }

    // One preset: a square of its colour, outlined in the accent colour when it's the current one.
    private class Swatch extends HibiscusButton {
        private final int rgb;

        Swatch(int rgb) {
            super(0, 0, SWATCH, SWATCH, Component.literal(format(rgb)), button -> {});
            this.rgb = rgb;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            pick(rgb);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            HibiscusTheme theme = theme();
            boolean current = rgb == color;
            int edge = current || isHoveredOrFocused() ? theme.accent() : theme.color(HibiscusTheme.PANEL_SHADOW);
            graphics.fill(getX(), getY(), getRight(), getBottom(), edge);
            graphics.fill(getX() + (current ? 2 : 1), getY() + (current ? 2 : 1), getRight() - (current ? 2 : 1),
                    getBottom() - (current ? 2 : 1), 0xFF000000 | rgb);
        }
    }
}
