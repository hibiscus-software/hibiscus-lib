/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.button;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;
import software.hibiscus.hibiscuslib.client.gui.widget.TextButton;
import software.hibiscus.hibiscuslib.client.gui.widget.ThemedScrollbar;
import software.hibiscus.hibiscuslib.config.HibiscusLibClientConfig;

// Opened by right-clicking a sidebar button: shows every button, lets the player tick which to show and drag rows
// to reorder them, and saves both to buttons.order and buttons.hidden in hibiscuslib-client.toml.
public class EditButtonsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER = 24;
    private static final int FOOTER = 32;

    private final @Nullable Screen parent;
    private final Map<Identifier, ButtonSpec> buttons;
    private final List<Identifier> rows = new ArrayList<>();
    private final Set<Identifier> hidden = new HashSet<>();

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int listTop;
    private int listHeight;
    private double scroll;
    private @Nullable Identifier dragging;
    private double dragY;

    public EditButtonsScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.hibiscuslib.edit_buttons"));
        this.parent = parent;
        this.buttons = HibiscusButtons.all();
        // Every button in its current order, hidden ones included (arranged without the hidden list).
        rows.addAll(HibiscusButtons.arrange(buttons, HibiscusLibClientConfig.order(), List.of()));
        for (String id : HibiscusLibClientConfig.hidden()) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier != null) {
                hidden.add(identifier);
            }
        }
    }

    @Override
    protected void init() {
        panelHeight = Mth.clamp(HEADER + FOOTER + rows.size() * ROW_HEIGHT + 8, 120, height - 20);
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - panelHeight) / 2;
        listTop = panelY + HEADER;
        listHeight = panelHeight - HEADER - FOOTER;
        int buttonY = panelY + panelHeight - 26;
        int buttonWidth = (PANEL_WIDTH - 24) / 3;
        addRenderableWidget(new TextButton(panelX + 8, buttonY, buttonWidth, 20, Component.translatable("gui.done"), button -> save()).accent());
        addRenderableWidget(new TextButton(panelX + 12 + buttonWidth, buttonY, buttonWidth, 20,
                Component.translatable("screen.hibiscuslib.edit_buttons.reset"), button -> reset()));
        addRenderableWidget(new TextButton(panelX + 16 + buttonWidth * 2, buttonY, buttonWidth, 20,
                Component.translatable("gui.cancel"), button -> onClose()));
        clampScroll();
    }

    private void save() {
        List<String> order = rows.stream().map(Identifier::toString).toList();
        List<String> hiddenIds = rows.stream().filter(hidden::contains).map(Identifier::toString).toList();
        HibiscusLibClientConfig.saveButtons(order, hiddenIds);
        onClose();
    }

    // Back to every button's default order, all shown (not saved until Done).
    private void reset() {
        hidden.clear();
        rows.clear();
        rows.addAll(HibiscusButtons.arrange(buttons, List.of(), List.of()));
        scroll = 0;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HibiscusTheme theme = HibiscusTheme.current();
        theme.panel(graphics, panelX, panelY, PANEL_WIDTH, panelHeight);
        graphics.centeredText(font, title, panelX + PANEL_WIDTH / 2, panelY + 8, theme.text());
        theme.inset(graphics, panelX + 6, listTop - 2, PANEL_WIDTH - 12, listHeight + 4);
        if (rows.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.hibiscuslib.edit_buttons.empty"),
                    panelX + PANEL_WIDTH / 2, listTop + listHeight / 2 - 4, theme.textMuted());
        }

        graphics.enableScissor(panelX + 6, listTop, panelX + PANEL_WIDTH - 6, listTop + listHeight);
        for (int i = 0; i < rows.size(); i++) {
            Identifier id = rows.get(i);
            if (id.equals(dragging)) {
                continue;
            }
            int y = rowY(i);
            if (y + ROW_HEIGHT < listTop || y > listTop + listHeight) {
                continue;
            }
            boolean hovered = dragging == null && isInList(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT;
            extractRow(graphics, theme, id, y, hovered);
        }
        graphics.disableScissor();
        int contentHeight = rows.size() * ROW_HEIGHT;
        if (contentHeight > listHeight) {
            int thumbHeight = Math.max(16, listHeight * listHeight / contentHeight);
            int thumbY = listTop + (int) (scroll * (listHeight - thumbHeight) / (contentHeight - listHeight));
            ThemedScrollbar.draw(graphics, panelX + PANEL_WIDTH - 7 - ThemedScrollbar.WIDTH, listTop, listHeight, thumbY, thumbHeight, false);
        }
        if (dragging != null) {
            graphics.nextStratum();
            int y = (int) dragY - ROW_HEIGHT / 2;
            graphics.fill(panelX + 8, y, panelX + PANEL_WIDTH - 8, y + ROW_HEIGHT, theme.color(HibiscusTheme.PANEL_HIGHLIGHT));
            extractRow(graphics, theme, dragging, y, true);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void extractRow(GuiGraphicsExtractor graphics, HibiscusTheme theme, Identifier id, int y, boolean hovered) {
        ButtonSpec spec = buttons.get(id);
        int left = panelX + 8;
        int right = panelX + PANEL_WIDTH - 10 - ThemedScrollbar.WIDTH;
        if (hovered) {
            graphics.fill(left, y, right, y + ROW_HEIGHT, theme.color(HibiscusTheme.HOVER));
        }
        boolean shown = !hidden.contains(id);
        // Drag handle
        for (int line = 0; line < 3; line++) {
            graphics.fill(left + 3, y + 6 + line * 3, left + 9, y + 7 + line * 3, theme.textMuted());
        }
        SidebarButton.drawIcon(graphics, spec.icon(), left + 13, y + 2);
        int textColor = shown ? theme.text() : theme.color(HibiscusTheme.TEXT_DISABLED);
        graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(spec.tooltip().get(), right - left - 70)), left + 34, y + 2, textColor, false);
        graphics.text(font, font.plainSubstrByWidth(id.toString(), right - left - 70), left + 34, y + 11, theme.textMuted(), false);
        // Show/hide toggle
        theme.draw(graphics, shown ? HibiscusTheme.SPRITE_TOGGLE_ON : HibiscusTheme.SPRITE_TOGGLE_OFF, right - 26, y + 4, 22, 12);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isInList(event.x(), event.y())) {
            int index = rowAt(event.y());
            if (index >= 0) {
                Identifier id = rows.get(index);
                if (event.x() >= panelX + PANEL_WIDTH - 34) {
                    if (!hidden.remove(id)) {
                        hidden.add(id);
                    }
                    playClick();
                } else {
                    dragging = id;
                    dragY = event.y();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragY = Mth.clamp(event.y(), listTop, listTop + listHeight);
            int target = Mth.clamp((int) ((dragY - listTop + scroll) / ROW_HEIGHT), 0, rows.size() - 1);
            int from = rows.indexOf(dragging);
            if (target != from) {
                rows.remove(from);
                rows.add(target, dragging);
            }
            // Scroll when dragging against the list's edges.
            if (event.y() < listTop + 6) {
                scroll -= 4;
            } else if (event.y() > listTop + listHeight - 6) {
                scroll += 4;
            }
            clampScroll();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (isInList(x, y)) {
            scroll -= scrollY * ROW_HEIGHT;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private void playClick() {
        net.minecraft.client.gui.components.AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
    }

    private void clampScroll() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() * ROW_HEIGHT - listHeight));
    }

    private int rowY(int index) {
        return listTop + index * ROW_HEIGHT - (int) scroll;
    }

    private int rowAt(double y) {
        int index = (int) ((y - listTop + scroll) / ROW_HEIGHT);
        return index >= 0 && index < rows.size() ? index : -1;
    }

    private boolean isInList(double x, double y) {
        return x >= panelX + 6 && x < panelX + PANEL_WIDTH - 6 && y >= listTop && y < listTop + listHeight;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
