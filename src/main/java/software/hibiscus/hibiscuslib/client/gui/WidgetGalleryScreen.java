/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.gui.popup.ConfirmDialog;
import software.hibiscus.hibiscuslib.client.gui.popup.ContextMenu;
import software.hibiscus.hibiscuslib.client.gui.widget.ColorPicker;
import software.hibiscus.hibiscuslib.client.gui.widget.IconButton;
import software.hibiscus.hibiscuslib.client.gui.widget.ItemSlotWidget;
import software.hibiscus.hibiscuslib.client.gui.widget.ListWidget;
import software.hibiscus.hibiscuslib.client.gui.widget.Panel;
import software.hibiscus.hibiscuslib.client.gui.widget.ScrollPanel;
import software.hibiscus.hibiscuslib.client.gui.widget.Tabs;
import software.hibiscus.hibiscuslib.client.gui.widget.TextArea;
import software.hibiscus.hibiscuslib.client.gui.widget.TextButton;
import software.hibiscus.hibiscuslib.client.gui.widget.TextInput;
import software.hibiscus.hibiscuslib.client.gui.widget.TextLayoutWidget;
import software.hibiscus.hibiscuslib.client.gui.widget.ToggleButton;

// Every Hibiscus widget on one screen, for checking the theme and how widgets behave at each GUI scale. Opened by the
// debug-only /hibiscuslib widgets client command.
public class WidgetGalleryScreen extends BaseScreen {
    private static final String KEY = "screen.hibiscuslib.gallery.";
    private static final Icon SETTINGS = Icon.sprite(HibiscusLib.id("icon/settings"));
    private static final Icon HIBISCUS = Icon.sprite(HibiscusLib.id("icon/hibiscus"));

    // Kept across tab switches and resizes, so the gallery shows that widgets keep their state.
    private int tab;
    private boolean toggleA = true;
    private boolean toggleB;
    private int color = 0xF2557A;
    private String notes = "";
    private @Nullable String selectedPlayer;
    private Component status = Component.empty();

    public WidgetGalleryScreen(@Nullable Screen parent) {
        super(Component.translatable(KEY + "title"), parent);
    }

    public int tab() {
        return tab;
    }

    // Switches tab from outside (the screenshot harness).
    public void showTab(int tab) {
        this.tab = tab;
        rebuildWidgets();
    }

    @Override
    protected int preferredWidth() {
        return 340;
    }

    @Override
    protected int preferredHeight() {
        return 250;
    }

    private static Component t(String key) {
        return Component.translatable(KEY + key);
    }

    @Override
    protected void initContent() {
        List<Tabs.Tab> tabs = List.of(
                new Tabs.Tab(t("tab.buttons"), Icon.NONE),
                new Tabs.Tab(t("tab.inputs"), Icon.NONE),
                new Tabs.Tab(t("tab.lists"), Icon.NONE),
                new Tabs.Tab(t("tab.text"), Icon.NONE),
                new Tabs.Tab(t("tab.popups"), Icon.NONE));
        addRenderableWidget(new Tabs(panelX + 6, panelY + HEADER - 4, panelWidth - 12, tabs, this::showTab)).setSelected(tab);
        int top = panelY + HEADER - 4 + Tabs.HEIGHT;
        ScrollPanel content = addRenderableWidget(new ScrollPanel(panelX + 6, top, panelWidth - 12, panelY + panelHeight - 6 - top, Panel.Style.INSET));
        int width = content.contentWidth();
        switch (tab) {
            case 0 -> buttons(content, width);
            case 1 -> inputs(content, width);
            case 2 -> lists(content, width);
            case 3 -> text(content, width);
            default -> popups(content, width);
        }
    }

    // A section heading; returns the y below it.
    private static int heading(ScrollPanel content, int width, int y, String key) {
        TextLayoutWidget heading = content.add(new TextLayoutWidget(0, 0, width, "## " + Component.translatable(KEY + key).getString()), 0, y);
        return y + heading.getHeight() + 3;
    }

    private void buttons(ScrollPanel content, int width) {
        int y = heading(content, width, 0, "section.text_buttons");
        int third = (width - 8) / 3;
        content.add(new TextButton(third, t("button.normal"), button -> status = t("status.pressed")), 0, y);
        content.add(new TextButton(third, t("button.accent"), button -> status = t("status.pressed")).accent(), third + 4, y);
        TextButton disabled = content.add(new TextButton(third, t("button.disabled"), button -> {}), (third + 4) * 2, y);
        disabled.active = false;
        y += 24;
        content.add(new TextButton(third * 2 + 4, t("button.with_icon"), button -> status = t("status.pressed")).icon(HIBISCUS), 0, y);
        y += 28;
        y = heading(content, width, y, "section.icon_buttons");
        content.add(new IconButton(SETTINGS, t("button.settings"), button -> status = t("button.settings")), 0, y);
        content.add(new IconButton(HIBISCUS, t("button.hibiscus"), button -> status = t("button.hibiscus")).flat(), 24, y);
        content.add(new IconButton(Icon.item(new ItemStack(Items.WRITABLE_BOOK)), t("button.book"), button -> status = t("button.book")), 48, y);
        IconButton off = content.add(new IconButton(SETTINGS, t("button.disabled"), button -> {}), 72, y);
        off.active = false;
        y += 28;
        y = heading(content, width, y, "section.toggles");
        content.add(new ToggleButton(0, 0, Math.min(width, 200), t("toggle.a"), toggleA, value -> toggleA = value), 0, y);
        y += 18;
        content.add(new ToggleButton(0, 0, Math.min(width, 200), t("toggle.b"), toggleB, value -> toggleB = value), 0, y);
    }

    private void inputs(ScrollPanel content, int width) {
        int y = heading(content, width, 0, "section.text_input");
        content.add(new TextInput(Math.min(width, 200), t("input.name")).placeholder(t("input.name.placeholder")), 0, y);
        y += 22;
        content.add(new TextInput(Math.min(width, 200), t("input.number")).placeholder(t("input.number.placeholder"))
                .validator(text -> text.isEmpty() || text.matches("\\d{1,6}")), 0, y);
        y += 26;
        y = heading(content, width, y, "section.text_area");
        TextArea area = new TextArea(0, 0, Math.min(width, 200), 48, t("input.notes"), t("input.notes.placeholder"));
        area.setValue(notes);
        area.setResponder(text -> notes = text);
        content.add(area, 0, y);
        y += 54;
        y = heading(content, width, y, "section.color_picker");
        content.add(new ColorPicker(0, 0, color, rgb -> color = rgb), 0, y);
        y += ColorPicker.HEIGHT + 6;
        y = heading(content, width, y, "section.item_slots");
        ItemStack[] stacks = {
                new ItemStack(Items.DIAMOND), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.ENCHANTED_BOOK),
                new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY
        };
        for (int i = 0; i < stacks.length; i++) {
            content.add(new ItemSlotWidget(0, 0, stacks[i]), i * (ItemSlotWidget.SIZE + 2), y);
        }
        y += ItemSlotWidget.SIZE + 4;
        content.add(new TextLayoutWidget(0, 0, width, Component.translatable(RecipeViewers.isAvailable()
                ? KEY + "item_slots.viewer" : KEY + "item_slots.no_viewer").getString()), 0, y);
    }

    private void lists(ScrollPanel content, int width) {
        int y = heading(content, width, 0, "section.list");
        List<String> players = List.of("Ada", "Basil", "Coral", "Dahlia", "Elm", "Fern", "Ginger", "Hazel", "Iris", "Juniper", "Kale", "Lotus");
        ListWidget<String> list = content.add(new ListWidget<>(0, 0, width, ListWidget.ROW_HEIGHT * 5 + 2,
                ListWidget.RowRenderer.<String>of(
                        name -> Icon.color(ColorPicker.PRESETS[Math.floorMod(name.hashCode(), ColorPicker.PRESETS.length)] | 0xFF000000),
                        Component::literal,
                        name -> Component.translatable(KEY + "list.subtitle", name.length()))), 0, y);
        list.setRows(players);
        list.emptyText(t("list.empty"));
        if (selectedPlayer != null) {
            list.select(selectedPlayer);
        }
        list.onSelect(name -> selectedPlayer = name);
        list.onActivate(name -> status = Component.translatable(KEY + "status.activated", name));
        list.onContextMenu((name, x, menuY) -> openPopup(ContextMenu.builder()
                .add(Component.translatable(KEY + "menu.message", name), () -> status = Component.translatable(KEY + "status.activated", name))
                .separator()
                .addDanger(t("menu.kick"), Icon.NONE, () -> status = t("menu.kick"))
                .build(x, menuY)));
        y += list.getHeight() + 6;
        ListWidget<String> empty = content.add(new ListWidget<>(0, 0, width, 40,
                ListWidget.RowRenderer.<String>of(name -> Icon.NONE, Component::literal, name -> null)), 0, y);
        empty.emptyText(t("list.empty"));
        y += 46;
        content.add(new TextLayoutWidget(0, 0, width, t("list.hint").getString()), 0, y);
        y += 24;
        y = heading(content, width, y, "section.faces");
        // The local player (online, with a dot) and two players the client doesn't know (default skins).
        java.util.List<Icon> faces = new java.util.ArrayList<>();
        if (minecraft.player != null) {
            faces.add(Icon.playerFace(minecraft.player.getUUID()).withDot(HibiscusTheme.current().color(HibiscusTheme.POSITIVE)));
        }
        faces.add(Icon.playerFace(java.util.UUID.nameUUIDFromBytes("hibiscus-demo-1".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        faces.add(Icon.playerFace(java.util.UUID.nameUUIDFromBytes("hibiscus-demo-2".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        for (int i = 0; i < faces.size(); i++) {
            content.add(new IconButton(faces.get(i), t("section.faces"), button -> {}).flat(), i * 24, y);
        }
        y += 24;
        content.add(new TextLayoutWidget(0, 0, width, t("faces.hint").getString()), 0, y);
    }

    private void text(ScrollPanel content, int width) {
        content.add(new TextLayoutWidget(0, 0, width, t("text.sample").getString()), 0, 0);
    }

    private void popups(ScrollPanel content, int width) {
        int y = heading(content, width, 0, "section.popups");
        int half = (width - 4) / 2;
        content.add(new TextButton(half, t("popup.confirm"), button -> openPopup(new ConfirmDialog(t("popup.confirm.title"),
                t("popup.confirm.message"), yes -> status = t(yes ? "status.confirmed" : "status.cancelled")))), 0, y);
        content.add(new TextButton(half, t("popup.danger"), button -> openPopup(new ConfirmDialog(t("popup.danger.title"),
                t("popup.danger.message"), t("popup.danger.confirm"), Component.translatable("gui.cancel"),
                yes -> status = t(yes ? "status.confirmed" : "status.cancelled")).dangerous())), half + 4, y);
        y += 24;
        content.add(new TextButton(half, t("popup.menu"), button -> openPopup(ContextMenu.builder()
                .add(t("menu.rename"), SETTINGS, () -> status = t("menu.rename"))
                .add(t("menu.color"), Icon.color(0xFF000000 | color), () -> status = t("menu.color"))
                .addDisabled(t("menu.promote"), Icon.NONE)
                .separator()
                .addDanger(t("menu.kick"), Icon.NONE, () -> status = t("menu.kick"))
                .build(button.getX(), button.getBottom()))), 0, y);
        content.add(new TextButton(half, t("popup.toast"), button -> HibiscusToasts.show(t("toast.title"), t("toast.message"),
                Icon.item(new ItemStack(Items.WRITABLE_BOOK)))), half + 4, y);
        y += 24;
        content.add(new TextButton(half, t("popup.typed"), button -> openPopup(new ConfirmDialog(t("popup.typed.title"),
                t("popup.typed.message"), t("popup.typed.confirm"), Component.translatable("gui.cancel"),
                yes -> status = t(yes ? "status.confirmed" : "status.cancelled")).dangerous().requireTyping("Coral"))), 0, y);
        y += 28;
        content.add(new TextLayoutWidget(0, 0, width, t("popup.hint").getString()), 0, y);
    }

    @Override
    protected void extractForeground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!status.getString().isEmpty()) {
            int x = panelX + panelWidth - 8 - font.width(status);
            graphics.text(font, status, Math.max(panelX + 8 + font.width(title) + 8, x), panelY + 7, HibiscusTheme.current().color(HibiscusTheme.POSITIVE), false);
        }
    }
}
