/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;


import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * Themed toasts in the top-right corner ("Quest complete", "Invited to Team Coral"). A toast with a token replaces
 * any showing toast with the same token instead of stacking, e.g. one "3 rewards waiting" toast that updates.
 */
public final class HibiscusToasts {
    private static final long DEFAULT_DURATION_MS = 5000;

    private HibiscusToasts() {}

    public static void show(Component title, @Nullable Component message, Icon icon) {
        show(title, message, icon, DEFAULT_DURATION_MS, null);
    }

    public static void show(Component title, @Nullable Component message, Icon icon, long durationMs, @Nullable Object token) {
        ToastManager toasts = Minecraft.getInstance().gui.toastManager();
        if (token != null) {
            HibiscusToast existing = toasts.getToast(HibiscusToast.class, token);
            if (existing != null) {
                existing.reset(title, message, icon);
                return;
            }
        }
        toasts.addToast(new HibiscusToast(title, message, icon, durationMs, token));
    }

    // Every Hibiscus toast is the same size: WIDTH wide and one vanilla toast slot (32 pixels) tall, laid out like
    // vanilla's (title, then one line of message; a longer message ends in an ellipsis). The panel stops GAP pixels
    // short of the slot, so stacked toasts sit evenly apart. (A taller toast would take two slots, and vanilla never
    // moves toasts up, so they'd stop lining up and leave big gaps as they go.)
    static final class HibiscusToast implements Toast {
        private static final int WIDTH = 240;
        private static final int GAP = 2;
        private static final int TEXT_X = 30;
        private static final int RIGHT_PADDING = 8;
        private static final String ELLIPSIS = "…";

        private final long durationMs;
        private final @Nullable Object token;
        private Component title;
        private @Nullable FormattedCharSequence line;
        private Icon icon;
        private boolean restart;
        private long shownSinceMs;
        private Visibility visibility = Visibility.SHOW;

        HibiscusToast(Component title, @Nullable Component message, Icon icon, long durationMs, @Nullable Object token) {
            this.durationMs = durationMs;
            this.token = token;
            reset(title, message, icon);
        }

        void reset(Component title, @Nullable Component message, Icon icon) {
            this.title = title;
            this.icon = icon;
            Font font = Minecraft.getInstance().font;
            this.line = message == null ? null : fit(font, message, WIDTH - TEXT_X - RIGHT_PADDING);
            this.restart = true;
        }

        // The message on one line, cut with an ellipsis if it's too long.
        private static FormattedCharSequence fit(Font font, Component message, int room) {
            if (font.width(message) <= room) {
                return message.getVisualOrderText();
            }
            FormattedText cut = font.substrByWidth(message, Math.max(0, room - font.width(ELLIPSIS)));
            return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(ELLIPSIS)));
        }

        @Override
        public Visibility getWantedVisibility() {
            return visibility;
        }

        @Override
        public void update(ToastManager manager, long fullyVisibleForMs) {
            if (restart) {
                shownSinceMs = fullyVisibleForMs;
                restart = false;
            }
            visibility = fullyVisibleForMs - shownSinceMs >= durationMs * manager.getNotificationDisplayTimeMultiplier()
                    ? Visibility.HIDE : Visibility.SHOW;
        }

        @Override
        public Object getToken() {
            return token != null ? token : NO_TOKEN;
        }

        @Override
        public int width() {
            return WIDTH;
        }

        @Override
        public int height() {
            return Toast.SLOT_HEIGHT;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
            HibiscusTheme theme = HibiscusTheme.current();
            int panelHeight = height() - GAP;
            theme.panel(graphics, 0, 0, WIDTH, panelHeight);
            graphics.fill(2, 2, 3, panelHeight - 2, theme.color(HibiscusTheme.TOOLTIP_ACCENT));
            icon.draw(graphics, 8, (panelHeight - 16) / 2);
            FormattedCharSequence heading = fit(font, title, WIDTH - TEXT_X - RIGHT_PADDING);
            if (line == null) {
                graphics.text(font, heading, TEXT_X, (panelHeight - 8) / 2, theme.text(), false);
                return;
            }
            graphics.text(font, heading, TEXT_X, 6, theme.text(), false);
            graphics.text(font, line, TEXT_X, 17, theme.textMuted(), false);
        }
    }
}
