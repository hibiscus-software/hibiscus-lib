/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
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

    static final class HibiscusToast implements Toast {
        private static final int WIDTH = 160;

        private final long durationMs;
        private final @Nullable Object token;
        private Component title;
        private List<FormattedCharSequence> lines;
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
            this.lines = message == null ? List.of() : font.split(message, WIDTH - 30);
            if (lines.size() > 2) {
                lines = lines.subList(0, 2);
            }
            this.restart = true;
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
            return Math.max(32, 12 + 10 * (1 + lines.size()));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
            HibiscusTheme theme = HibiscusTheme.current();
            theme.panel(graphics, 0, 0, width(), height());
            graphics.fill(2, 2, 3, height() - 2, theme.color(HibiscusTheme.TOOLTIP_ACCENT));
            icon.draw(graphics, 8, (height() - 16) / 2);
            graphics.text(font, title, 30, 7, theme.text(), false);
            int y = 18;
            for (FormattedCharSequence line : lines) {
                graphics.text(font, line, 30, y, theme.textMuted(), false);
                y += 10;
            }
        }
    }
}
