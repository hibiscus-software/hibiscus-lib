/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import software.hibiscus.hibiscuslib.client.gui.BaseScreen;
import software.hibiscus.hibiscuslib.client.gui.TextLayout;

// Shows a TextLayout at a fixed width, as tall as the text; put it in a ScrollPanel for long text. Links under the
// mouse show where they go and open (after asking) when clicked; inline item icons show the item's tooltip. A click
// handler (onClickEvent) gets first say over clicked text, for tokens that link into the caller's own screen.
public class TextLayoutWidget extends HibiscusWidget {
    private final TextLayout layout;
    private final String plainText;
    private Predicate<ClickEvent> clickHandler = event -> false;

    public TextLayoutWidget(int x, int y, int width, String markup) {
        this(x, y, width, markup, TextLayout.Tokens.NONE);
    }

    // With the caller's own {name:argument} tokens (see TextLayout.parse(String, Tokens)).
    public TextLayoutWidget(int x, int y, int width, String markup, TextLayout.Tokens tokens) {
        super(x, y, width, 0, Component.empty());
        this.layout = TextLayout.parse(markup, tokens);
        this.plainText = markup;
        setHeight(Math.max(1, layout.height(font(), width)));
    }

    // Called with the click event of clicked text before the screen handles it; return true to take it. Use it for
    // ClickEvent.Custom events from your own tokens, which the screen would otherwise send to the server.
    public TextLayoutWidget onClickEvent(Predicate<ClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (layout != null) {
            setHeight(Math.max(1, layout.height(font(), width)));
        }
    }

    private Optional<Style> styleAt(double mouseX, double mouseY) {
        return layout.styleAt(font(), getWidth(), mouseX - getX(), mouseY - getY());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        layout.draw(graphics, font(), getX(), getY(), getWidth(), theme().text());
        if (isHovered()) {
            Optional<ItemStack> item = layout.itemAt(font(), getWidth(), mouseX - getX(), mouseY - getY());
            if (item.isPresent()) {
                graphics.setTooltipForNextFrame(font(), item.get(), mouseX, mouseY);
                return;
            }
            styleAt(mouseX, mouseY).map(Style::getHoverEvent).ifPresent(hover -> {
                if (hover instanceof HoverEvent.ShowText(Component text)) {
                    graphics.setTooltipForNextFrame(font(), text, mouseX, mouseY);
                }
            });
        }
    }

    // Only takes clicks that land on a link, so clicks on plain text fall through.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y()) || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        Optional<Style> style = styleAt(event.x(), event.y());
        if (style.isEmpty() || style.get().getClickEvent() == null) {
            return false;
        }
        if (clickHandler.test(style.get().getClickEvent())) {
            return true;
        }
        if (Minecraft.getInstance().gui.screen() instanceof BaseScreen screen) {
            screen.handleClickEvent(style.get().getClickEvent());
            return true;
        }
        return false;
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return false;
    }

    // Text isn't a Tab stop.
    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(plainText));
    }
}
