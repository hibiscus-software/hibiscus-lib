/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.popup;

import java.util.List;

import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.widget.TextButton;
import software.hibiscus.hibiscuslib.client.gui.widget.TextInput;

/**
 * A yes/no question over the screen ("Leave the team?", "Reset this quest?"). The callback gets true for the confirm
 * button and false for cancel, Esc or a right click outside. Mark it {@link #dangerous()} to underline the confirm
 * button in the negative colour, and for actions that can't be undone (disbanding a team, deleting a chapter) add
 * {@link #requireTyping(String)}: the confirm button stays disabled until the player types the given text.
 */
public class ConfirmDialog extends Popup {
    private static final int WIDTH = 220;
    private static final int PADDING = 8;
    // The hint line, the text field and a gap, when typing is required.
    private static final int TYPING_HEIGHT = 10 + 18 + 6;

    private final Component title;
    private final Component message;
    private final Component confirmLabel;
    private final Component cancelLabel;
    private final BooleanConsumer callback;
    private final List<FormattedCharSequence> lines;
    private boolean dangerous;
    private @Nullable String requiredText;
    private @Nullable TextButton confirm;
    private @Nullable TextInput typed;

    public ConfirmDialog(Component title, Component message, BooleanConsumer callback) {
        this(title, message, CommonComponents.GUI_YES, CommonComponents.GUI_NO, callback);
    }

    public ConfirmDialog(Component title, Component message, Component confirmLabel, Component cancelLabel, BooleanConsumer callback) {
        super(WIDTH, 80);
        this.title = title;
        this.message = message;
        this.confirmLabel = confirmLabel;
        this.cancelLabel = cancelLabel;
        this.callback = callback;
        // The panel is drawn here, under the title and message, so the base draws only the buttons.
        setStyle(Style.NONE);
        Font font = Minecraft.getInstance().font;
        this.lines = font.split(message, WIDTH - PADDING * 2);
        setHeight(PADDING + 12 + lines.size() * 10 + 8 + 20 + PADDING);
    }

    public ConfirmDialog dangerous() {
        this.dangerous = true;
        return this;
    }

    // Asks the player to type text (usually the name of what's being deleted) before Confirm works. Exact match,
    // ignoring spaces at either end. Call before opening the dialog.
    public ConfirmDialog requireTyping(String text) {
        if (requiredText == null) {
            setHeight(getHeight() + TYPING_HEIGHT);
        }
        this.requiredText = text.strip();
        return this;
    }

    private boolean typedMatches() {
        return requiredText == null || (typed != null && typed.getValue().strip().equals(requiredText));
    }

    @Override
    protected void init() {
        int buttonWidth = (getWidth() - PADDING * 3) / 2;
        int y = getHeight() - PADDING - 20;
        confirm = add(new TextButton(buttonWidth, confirmLabel, button -> {
            if (typedMatches()) {
                answer(true);
            }
        }), PADDING, y);
        if (!dangerous) {
            confirm.accent();
        }
        add(new TextButton(buttonWidth, cancelLabel, button -> answer(false)), PADDING * 2 + buttonWidth, y);
        if (requiredText != null) {
            typed = add(new TextInput(getWidth() - PADDING * 2, Component.translatable("gui.hibiscuslib.confirm.type", requiredText)),
                    PADDING, y - 6 - 18);
            typed.setMaxLength(Math.max(32, requiredText.length() + 8));
            typed.placeholder(Component.literal(requiredText));
            typed.setResponder(text -> confirm.active = typedMatches());
            confirm.active = false;
            setFocused(typed);
        }
    }

    private void answer(boolean confirmed) {
        close();
        callback.accept(confirmed);
    }

    @Override
    protected void onCancel() {
        callback.accept(false);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        Font font = Minecraft.getInstance().font;
        theme.panel(graphics, getX(), getY(), getWidth(), getHeight());
        graphics.fill(getX() + 2, getY() + 2, getRight() - 2, getY() + 3, dangerous ? theme.color(HibiscusTheme.NEGATIVE) : theme.accent());
        graphics.text(font, title, getX() + PADDING, getY() + PADDING, theme.text(), false);
        int y = getY() + PADDING + 14;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, getX() + PADDING, y, theme.textMuted(), false);
            y += 10;
        }
        if (requiredText != null && typed != null) {
            Component hint = Component.translatable("gui.hibiscuslib.confirm.type", Component.literal(requiredText).withColor(theme.text()));
            graphics.text(font, hint, getX() + PADDING, typed.getY() - 11, theme.textMuted(), false);
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        if (dangerous && confirm != null) {
            // Draw a red rule under the confirm button, so a dangerous action stands out without relying on colour alone.
            graphics.fill(confirm.getX() + 3, confirm.getBottom() - 3, confirm.getRight() - 3, confirm.getBottom() - 2,
                    theme.color(HibiscusTheme.NEGATIVE));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty().append(title).append(". ").append(message));
        if (requiredText != null) {
            output.add(NarratedElementType.HINT, Component.translatable("gui.hibiscuslib.confirm.type", requiredText));
        }
        super.updateWidgetNarration(output);
    }
}
