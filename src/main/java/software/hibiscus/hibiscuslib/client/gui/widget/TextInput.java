/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// A themed one-line text field with a placeholder and an optional validator: while the text fails it, the text
// and border turn the theme's negative colour. Vanilla's EditBox does the editing (selection, clipboard, IME).
public class TextInput extends EditBox {
    private static final int PADDING = 4;

    private Predicate<String> validator = text -> true;
    private Consumer<String> listener = text -> {};
    private boolean valid = true;

    public TextInput(int x, int y, int width, int height, Component narration) {
        super(Minecraft.getInstance().font, x, y, width, height, narration);
        setBordered(true);
        super.setResponder(this::changed);
        restyle();
    }

    public TextInput(int width, Component narration) {
        this(0, 0, width, 18, narration);
    }

    // Shown, muted, while the field is empty and unfocused.
    public TextInput placeholder(Component placeholder) {
        setHint(placeholder.copy().withColor(HibiscusTheme.current().textMuted()));
        return this;
    }

    public TextInput validator(Predicate<String> validator) {
        this.validator = validator;
        changed(getValue());
        return this;
    }

    // Called with the text on every change (whatever the validator says).
    @Override
    public void setResponder(Consumer<String> listener) {
        this.listener = listener;
    }

    public boolean isValid() {
        return valid;
    }

    private void changed(String text) {
        valid = validator.test(text);
        restyle();
        listener.accept(text);
    }

    private void restyle() {
        HibiscusTheme theme = HibiscusTheme.current();
        setTextColor(valid ? theme.text() : theme.color(HibiscusTheme.NEGATIVE));
        setTextColorUneditable(theme.color(HibiscusTheme.TEXT_DISABLED));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!isVisible()) {
            return;
        }
        HibiscusTheme theme = HibiscusTheme.current();
        int x = getX();
        int y = getY();
        int width = getWidth();
        theme.draw(graphics, isFocused() ? HibiscusTheme.SPRITE_TEXT_FIELD_FOCUSED : HibiscusTheme.SPRITE_TEXT_FIELD, x, y, width, getHeight());
        if (!valid) {
            graphics.outline(x, y, width, getHeight(), theme.color(HibiscusTheme.NEGATIVE));
        }
        // EditBox draws vanilla's sprite when bordered, so draw the text unbordered, moved inside the padding
        // (the same place a bordered box puts it, so clicks still land on the right character).
        setBordered(false);
        setX(x + PADDING);
        setY(y + (getHeight() - 8) / 2);
        setWidth(width - PADDING * 2);
        try {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        } finally {
            setWidth(width);
            setBordered(true);
            setX(x);
            setY(y);
        }
    }
}
