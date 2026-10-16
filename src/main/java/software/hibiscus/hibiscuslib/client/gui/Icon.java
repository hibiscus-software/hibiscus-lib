/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import software.hibiscus.hibiscuslib.button.ButtonIcon;

// Anything drawn in a 16x16 icon spot: a GUI sprite, an item, or custom drawing (a player face, a coloured dot).
@FunctionalInterface
public interface Icon {
    Icon NONE = (graphics, x, y) -> {};

    void draw(GuiGraphicsExtractor graphics, int x, int y);

    static Icon sprite(Identifier sprite) {
        return (graphics, x, y) -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16);
    }

    static Icon item(ItemStack stack) {
        return (graphics, x, y) -> graphics.item(stack, x, y);
    }

    static Icon item(Supplier<ItemStack> stack) {
        return (graphics, x, y) -> graphics.item(stack.get(), x, y);
    }

    // A filled square of one colour, with the theme's shadow edge (team colours, swatches).
    static Icon color(int argb) {
        return (graphics, x, y) -> {
            graphics.fill(x + 2, y + 2, x + 14, y + 14, HibiscusTheme.current().color(HibiscusTheme.PANEL_SHADOW));
            graphics.fill(x + 3, y + 3, x + 13, y + 13, argb);
        };
    }

    static Icon of(ButtonIcon icon) {
        return switch (icon) {
            case ButtonIcon.Sprite sprite -> sprite(sprite.sprite());
            case ButtonIcon.Item item -> item(item.stack());
        };
    }
}
