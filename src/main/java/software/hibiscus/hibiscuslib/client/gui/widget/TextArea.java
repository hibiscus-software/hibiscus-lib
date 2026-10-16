/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// A themed multi-line text field (quest descriptions, notes, anything longer than a line), with a placeholder while
// it's empty. Vanilla's MultiLineEditBox does the editing inside it (wrapping, selection, clipboard, IME, scrolling);
// this draws the theme's text-field frame around it and the theme's scrollbar beside the text.
public class TextArea extends AbstractWidget {
    private static final int SCROLLBAR_GAP = 2;

    private final MultiLineEditBox box;
    private Consumer<String> listener = text -> {};

    public TextArea(int x, int y, int width, int height, Component narration, Component placeholder) {
        super(x, y, width, height, narration);
        HibiscusTheme theme = HibiscusTheme.current();
        box = MultiLineEditBox.builder()
                .setPlaceholder(placeholder.copy().withColor(theme.textMuted()))
                .setTextColor(theme.text())
                .setCursorColor(theme.text())
                .setTextShadow(false)
                .setShowBackground(false)
                .setShowDecorations(false)
                .build(Minecraft.getInstance().font, boxWidth(width), height, narration);
        box.setValueListener(text -> listener.accept(text));
        place();
    }

    public TextArea(int x, int y, int width, int height, Component narration) {
        this(x, y, width, height, narration, Component.empty());
    }

    // The text box leaves room on the right for the scrollbar (vanilla draws its own just outside the box; the theme's
    // is drawn over that strip instead).
    private static int boxWidth(int width) {
        return Math.max(16, width - ThemedScrollbar.WIDTH - SCROLLBAR_GAP);
    }

    private void place() {
        box.setX(getX());
        box.setY(getY());
        box.setWidth(boxWidth(getWidth()));
        box.setHeight(getHeight());
    }

    public String getValue() {
        return box.getValue();
    }

    public void setValue(String text) {
        box.setValue(text, true);
    }

    // Called with the text on every change.
    public TextArea setResponder(Consumer<String> listener) {
        this.listener = listener;
        return this;
    }

    public TextArea setCharacterLimit(int limit) {
        box.setCharacterLimit(limit);
        return this;
    }

    public double scrollAmount() {
        return box.scrollAmount();
    }

    public void setScrollAmount(double amount) {
        box.setScrollAmount(amount);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        place();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        place();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        place();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        place();
    }

    // ---- drawing ----

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        theme.draw(graphics, isFocused() ? HibiscusTheme.SPRITE_TEXT_FIELD_FOCUSED : HibiscusTheme.SPRITE_TEXT_FIELD,
                getX(), getY(), getWidth(), getHeight());
        // Vanilla's scrollbar sits just right of the box: keep it out, and draw the theme's there instead.
        graphics.enableScissor(getX(), getY(), box.getRight(), getBottom());
        box.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.disableScissor();
        int max = box.maxScrollAmount();
        if (max > 0) {
            int track = getHeight() - 2;
            int content = max + getHeight();
            int thumb = Mth.clamp(track * getHeight() / content, 8, track);
            int thumbY = getY() + 1 + (int) Math.round((track - thumb) * box.scrollAmount() / max);
            boolean hot = mouseX >= box.getRight() && mouseX < getRight() && mouseY >= getY() && mouseY < getBottom();
            ThemedScrollbar.draw(graphics, getRight() - ThemedScrollbar.WIDTH - 1, getY() + 1, track, thumbY, thumb, hot);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        box.updateNarration(output);
    }

    // ---- input: all of it goes to the box ----

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return active && visible && mouseX >= getX() && mouseY >= getY() && mouseX < getRight() && mouseY < getBottom();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        box.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return box.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return box.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return isMouseOver(mouseX, mouseY) && box.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return box.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return box.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return box.preeditUpdated(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        box.setFocused(focused);
    }

    @Override
    public boolean capturesInput() {
        return box.capturesInput();
    }
}
