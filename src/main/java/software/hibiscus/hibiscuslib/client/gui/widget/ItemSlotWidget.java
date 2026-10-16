/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.RecipeViewers;

// An 18x18 slot showing an item stack (with its count and the item's own tooltip). Without a click handler, a left
// click (or R while focused) opens the recipe viewer on the item's recipes and a right click (or U) on its uses; JEI's
// own keys work over it too, through BaseScreen. With a handler, clicks go to the handler instead.
public class ItemSlotWidget extends HibiscusWidget {
    public static final int SIZE = 18;

    private final Supplier<ItemStack> stack;
    private @Nullable Consumer<MouseButtonEvent> onClick;

    public ItemSlotWidget(int x, int y, Supplier<ItemStack> stack) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.stack = stack;
    }

    public ItemSlotWidget(int x, int y, ItemStack stack) {
        this(x, y, () -> stack);
    }

    public ItemSlotWidget onClick(Consumer<MouseButtonEvent> onClick) {
        this.onClick = onClick;
        return this;
    }

    public ItemStack stack() {
        return stack.get();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = theme();
        theme.inset(graphics, getX(), getY(), SIZE, SIZE);
        ItemStack item = stack.get();
        if (!item.isEmpty()) {
            graphics.item(item, getX() + 1, getY() + 1);
            graphics.itemDecorations(font(), item, getX() + 1, getY() + 1);
        }
        if (isHoveredOrFocused()) {
            graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, theme.color(HibiscusTheme.HOVER));
        }
        if (isHovered() && !item.isEmpty()) {
            graphics.setTooltipForNextFrame(font(), item, mouseX, mouseY);
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT || buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (onClick != null) {
            onClick.accept(event);
        } else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            RecipeViewers.showUses(stack.get());
        } else {
            RecipeViewers.showRecipes(stack.get());
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_R) {
            return RecipeViewers.showRecipes(stack.get());
        }
        if (event.key() == InputConstants.KEY_U) {
            return RecipeViewers.showUses(stack.get());
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        ItemStack item = stack.get();
        if (!item.isEmpty()) {
            output.add(NarratedElementType.TITLE, Component.translatable("gui.hibiscuslib.item_slot.narration",
                    item.getCount(), item.getHoverName()));
        }
    }

    // Plays no click sound when there's nothing for the click to do.
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
        if (onClick != null || RecipeViewers.isAvailable()) {
            super.playDownSound(soundManager);
        }
    }
}
