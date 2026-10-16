/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// The theme's scrollbar, shared by ScrollPanel and ListWidget: a recessed track and a thumb that lights up in the
// accent colour while hovered or dragged.
public final class ThemedScrollbar {
    public static final int WIDTH = 6;
    static final AbstractScrollArea.ScrollbarSettings SETTINGS = AbstractScrollArea.defaultSettings(12);

    private ThemedScrollbar() {}

    public static void draw(GuiGraphicsExtractor graphics, int x, int top, int height, int thumbY, int thumbHeight, boolean hot) {
        HibiscusTheme theme = HibiscusTheme.current();
        theme.draw(graphics, HibiscusTheme.SPRITE_SCROLLBAR_TRACK, x, top, WIDTH, height);
        theme.draw(graphics, hot ? HibiscusTheme.SPRITE_SCROLLBAR_THUMB_HOVER : HibiscusTheme.SPRITE_SCROLLBAR_THUMB, x, thumbY, WIDTH, thumbHeight);
    }
}
