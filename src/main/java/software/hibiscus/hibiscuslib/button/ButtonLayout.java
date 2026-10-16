/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Where each sidebar button goes, as plain arithmetic (no client classes, so gametests can check it).
 *
 * <p>Buttons fill a column as tall as the panel, then wrap into further columns away from the panel. When the screen
 * has no room for that many columns (a narrow window, or the recipe book open beside the inventory), columns grow
 * taller instead, up to the screen's height, moving up if they must; buttons that still don't fit are left out.
 */
public final class ButtonLayout {
    public static final int SIZE = 16;
    // Space per button: 16 px and a 2 px gap.
    public static final int STEP = SIZE + 2;
    // Gap between the bar and the panel.
    public static final int MARGIN = 4;
    // Gap kept at the screen's edges.
    public static final int EDGE = 2;

    /** The panel the bar sits beside, and the screen, in GUI pixels. */
    public record Area(int left, int top, int right, int panelHeight, int screenWidth, int screenHeight) {}

    private ButtonLayout() {}

    /** The top-left corner of each of {@code count} buttons, in order; null for a button that doesn't fit. */
    public static List<int @Nullable []> place(int count, ButtonLocation location, Area area) {
        List<int @Nullable []> positions = new ArrayList<>(count);
        if (count <= 0) {
            return positions;
        }
        int maxRows = Math.max(1, (area.screenHeight() - EDGE * 2 + 2) / STEP);
        int panelRows = Math.min(maxRows, Math.max(1, (area.panelHeight() + 2) / STEP));
        int roomColumns = Math.max(1, (switch (location) {
            case TOP_RIGHT -> area.screenWidth() - EDGE - area.right() - MARGIN;
            case LEFT_COLUMN -> area.left() - MARGIN - EDGE;
            default -> area.left() - MARGIN - EDGE;
        } + 2) / STEP);
        int rows = panelRows;
        if (ceilDiv(count, rows) > roomColumns) {
            rows = Math.min(maxRows, ceilDiv(count, roomColumns));
        }
        int columnHeight = rows * STEP - 2;
        // A column that would run off the bottom (or top) of the screen moves to fit.
        int top = Math.max(EDGE, Math.min(area.top(), area.screenHeight() - EDGE - columnHeight));
        int bottom = Math.min(area.screenHeight() - EDGE, Math.max(area.top() + area.panelHeight(), EDGE + columnHeight));
        for (int i = 0; i < count; i++) {
            int column = i / rows;
            int row = i % rows;
            if (column >= roomColumns) {
                positions.add(null);
                continue;
            }
            int x = switch (location) {
                case TOP_RIGHT -> area.right() + MARGIN + column * STEP;
                case LEFT_COLUMN -> EDGE + column * STEP;
                default -> area.left() - MARGIN - SIZE - column * STEP;
            };
            int y = location == ButtonLocation.BOTTOM_LEFT ? bottom - SIZE - row * STEP : top + row * STEP;
            positions.add(new int[] {x, y});
        }
        return positions;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
