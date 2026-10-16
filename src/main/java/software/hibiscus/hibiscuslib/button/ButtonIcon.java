/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// What a sidebar button shows: a GUI sprite (assets/<ns>/textures/gui/sprites/<path>.png) or an item stack, drawn
// 16x16. The stack is a supplier so a button can follow state (a quest book that changes when rewards are waiting).
public sealed interface ButtonIcon {
    record Sprite(Identifier sprite) implements ButtonIcon {}

    record Item(Supplier<ItemStack> stack) implements ButtonIcon {}

    static ButtonIcon sprite(Identifier sprite) {
        return new Sprite(sprite);
    }

    static ButtonIcon item(Supplier<ItemStack> stack) {
        return new Item(stack);
    }
}
