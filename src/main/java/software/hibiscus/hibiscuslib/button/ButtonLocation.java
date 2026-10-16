/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

// Where the sidebar button bar sits around the inventory (buttons.location in hibiscuslib-client.toml).
public enum ButtonLocation {
    // A column left of the inventory panel, from its top down; further columns go left.
    TOP_LEFT,
    // A column right of the inventory panel, from its top down; further columns go right.
    TOP_RIGHT,
    // A column left of the inventory panel, from its bottom up; further columns go left.
    BOTTOM_LEFT,
    // A column along the left edge of the screen, from the panel's top down; further columns go right.
    LEFT_COLUMN
}
