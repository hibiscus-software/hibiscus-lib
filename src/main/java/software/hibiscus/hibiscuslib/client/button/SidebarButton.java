/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.button;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.button.ButtonBadge;
import software.hibiscus.hibiscuslib.button.ButtonIcon;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// One 16x16 sidebar button. A real widget, so Tab focuses it, Enter/Space presses it and the narrator reads its
// tooltip. Left click runs the button's action; right click opens the edit screen.
public class SidebarButton extends AbstractButton {
    public static final int SIZE = 16;

    private final Identifier id;
    private final ButtonSpec spec;

    public SidebarButton(Identifier id, ButtonSpec spec, int x, int y) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.id = id;
        this.spec = spec;
    }

    public Identifier id() {
        return id;
    }

    public ButtonSpec spec() {
        return spec;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        try {
            spec.onClick().run();
        } catch (RuntimeException e) {
            HibiscusLib.LOGGER.error("Sidebar button {} failed", id, e);
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT || buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.setScreen(new EditButtonsScreen(minecraft.gui.screen()));
        } else {
            onPress(event);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        Component tooltip = spec.tooltip().get();
        setMessage(tooltip);
        if (isHoveredOrFocused()) {
            theme.draw(graphics, HibiscusTheme.SPRITE_SIDEBAR_HOVER, getX() - 1, getY() - 1, SIZE + 2, SIZE + 2);
        }
        drawIcon(graphics, spec.icon(), getX(), getY());
        drawBadge(graphics, spec.badge().get(), getX(), getY(), theme);
        if (isHovered() && !tooltip.getString().isEmpty()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    // Shared with the edit screen, which draws the same icons.
    static void drawIcon(GuiGraphicsExtractor graphics, ButtonIcon icon, int x, int y) {
        switch (icon) {
            case ButtonIcon.Sprite sprite -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.sprite(), x, y, SIZE, SIZE);
            case ButtonIcon.Item item -> graphics.item(item.stack().get(), x, y);
        }
    }

    // A count in the bottom-right corner, or a dot in the top-right one.
    static void drawBadge(GuiGraphicsExtractor graphics, ButtonBadge badge, int x, int y, HibiscusTheme theme) {
        if (!ButtonBadge.isShown(badge)) {
            return;
        }
        int color = badge.color() == ButtonBadge.THEME_COLOR ? theme.accent() : badge.color();
        if (badge.plainDot()) {
            graphics.fill(x + SIZE - 5, y, x + SIZE, y + 5, theme.color(HibiscusTheme.PANEL_SHADOW));
            graphics.fill(x + SIZE - 4, y + 1, x + SIZE - 1, y + 4, color);
            return;
        }
        Font font = Minecraft.getInstance().font;
        String label = badge.label();
        int width = font.width(label) + 2;
        int left = x + SIZE - width + 1;
        int top = y + SIZE - 8;
        graphics.nextStratum();
        graphics.fill(left - 1, top - 1, x + SIZE + 2, y + SIZE + 1, theme.color(HibiscusTheme.PANEL_SHADOW));
        graphics.fill(left, top, x + SIZE + 1, y + SIZE, color);
        graphics.text(font, label, left + 1, top, 0xFFFFFFFF, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
