/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import software.hibiscus.hibiscuslib.button.ButtonIcon;

// Anything drawn in a 16x16 icon spot: a GUI sprite, an item, a player's face, or custom drawing.
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

    // A player's face (with their hat layer): their skin if the client knows them (they're on the server, or the skin
    // has been fetched), else the default skin for their UUID until it arrives. Works for offline players.
    static Icon playerFace(UUID player) {
        ResolvableProfile profile = ResolvableProfile.createUnresolved(player);
        return (graphics, x, y) -> {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            PlayerInfo info = connection != null ? connection.getPlayerInfo(player) : null;
            if (info != null) {
                PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), x, y, 16);
            } else {
                PlayerFaceExtractor.extractRenderState(graphics, profile, x, y, 16);
            }
        };
    }

    // This icon with a small dot in its bottom-right corner, e.g. the theme's positive colour for "online".
    default Icon withDot(int argb) {
        return (graphics, x, y) -> {
            draw(graphics, x, y);
            graphics.fill(x + 11, y + 11, x + 16, y + 16, HibiscusTheme.current().color(HibiscusTheme.PANEL_SHADOW));
            graphics.fill(x + 12, y + 12, x + 15, y + 15, argb);
        };
    }

    static Icon of(ButtonIcon icon) {
        return switch (icon) {
            case ButtonIcon.Sprite sprite -> sprite(sprite.sprite());
            case ButtonIcon.Item item -> item(item.stack());
        };
    }
}
