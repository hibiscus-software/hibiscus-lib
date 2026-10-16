/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

import org.jspecify.annotations.Nullable;

// The small marker on a sidebar button's corner: a count (unclaimed rewards, pending invites) or a plain dot.
// A badge supplier returns null, or NONE, for no badge.
public record ButtonBadge(int count, boolean plainDot, int color) {
    public static final ButtonBadge NONE = new ButtonBadge(0, false, 0);

    // The theme's accent colour, picked when the badge is drawn.
    public static final int THEME_COLOR = 0;

    public static ButtonBadge count(int count) {
        return count > 0 ? new ButtonBadge(count, false, THEME_COLOR) : NONE;
    }

    public static ButtonBadge count(int count, int color) {
        return count > 0 ? new ButtonBadge(count, false, color) : NONE;
    }

    public static ButtonBadge dot() {
        return new ButtonBadge(0, true, THEME_COLOR);
    }

    public static ButtonBadge dot(int color) {
        return new ButtonBadge(0, true, color);
    }

    public boolean isShown() {
        return plainDot || count > 0;
    }

    public static boolean isShown(@Nullable ButtonBadge badge) {
        return badge != null && badge.isShown();
    }

    // What's written on the badge: the count, capped at 99+.
    public String label() {
        return count > 99 ? "99+" : Integer.toString(count);
    }
}
