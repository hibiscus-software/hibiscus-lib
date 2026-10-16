/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import software.hibiscus.hibiscuslib.HibiscusLib;

/**
 * The colours and nine-slice sprites every Hibiscus screen draws with, so Teams, Quests and anything else built on
 * Hibiscus Lib look like one family, and a resource pack can restyle them all.
 *
 * <p>Loaded from {@code assets/hibiscuslib/hibiscus_theme/default.json}; a resource pack overrides it by shipping that
 * file (the whole file, or only the keys it changes, since missing keys keep the built-in values), and can replace the
 * sprites under {@code assets/hibiscuslib/textures/gui/sprites/} like any texture.
 */
public final class HibiscusTheme {
    // Named colours, ARGB.
    public static final String TEXT = "text";
    public static final String TEXT_MUTED = "text_muted";
    public static final String TEXT_DISABLED = "text_disabled";
    public static final String ACCENT = "accent";
    public static final String ACCENT_HOVER = "accent_hover";
    public static final String POSITIVE = "positive";
    public static final String WARNING = "warning";
    public static final String NEGATIVE = "negative";
    public static final String GOLD = "gold";
    public static final String PANEL = "panel";
    public static final String PANEL_HIGHLIGHT = "panel_highlight";
    public static final String PANEL_SHADOW = "panel_shadow";
    public static final String RECESS = "recess";
    public static final String TOOLTIP_ACCENT = "tooltip_accent";
    public static final String SELECTION = "selection";
    public static final String HOVER = "hover";
    public static final String OVERLAY = "overlay";

    // Sprite keys.
    public static final String SPRITE_PANEL = "panel";
    public static final String SPRITE_INSET = "inset";
    public static final String SPRITE_BUTTON = "button";
    public static final String SPRITE_BUTTON_HOVER = "button_hover";
    public static final String SPRITE_BUTTON_PRESSED = "button_pressed";
    public static final String SPRITE_BUTTON_DISABLED = "button_disabled";
    public static final String SPRITE_TEXT_FIELD = "text_field";
    public static final String SPRITE_TEXT_FIELD_FOCUSED = "text_field_focused";
    public static final String SPRITE_SCROLLBAR_TRACK = "scrollbar_track";
    public static final String SPRITE_SCROLLBAR_THUMB = "scrollbar_thumb";
    public static final String SPRITE_SCROLLBAR_THUMB_HOVER = "scrollbar_thumb_hover";
    public static final String SPRITE_TAB = "tab";
    public static final String SPRITE_TAB_SELECTED = "tab_selected";
    public static final String SPRITE_SIDEBAR_HOVER = "sidebar_hover";
    public static final String SPRITE_TOGGLE_OFF = "toggle_off";
    public static final String SPRITE_TOGGLE_ON = "toggle_on";

    // The built-in default theme: a neutral dark UI with a hibiscus coral accent. default.json repeats these values.
    private static final Map<String, Integer> DEFAULT_COLORS = Map.ofEntries(
            Map.entry(PANEL, 0xFF2B2D31),
            Map.entry(PANEL_HIGHLIGHT, 0xFF3F4248),
            Map.entry(PANEL_SHADOW, 0xFF18191C),
            Map.entry(RECESS, 0xFF1E1F22),
            Map.entry(TEXT, 0xFFE6E6E6),
            Map.entry(TEXT_MUTED, 0xFF9A9DA3),
            Map.entry(TEXT_DISABLED, 0xFF5C5F66),
            Map.entry(ACCENT, 0xFFF2557A),
            Map.entry(ACCENT_HOVER, 0xFFFF7898),
            Map.entry(POSITIVE, 0xFF4FC78A),
            Map.entry(WARNING, 0xFFF2C14E),
            Map.entry(NEGATIVE, 0xFFE5484D),
            Map.entry(GOLD, 0xFFE8B04B),
            Map.entry(TOOLTIP_ACCENT, 0xFFF2557A),
            Map.entry(SELECTION, 0x59F2557A),
            Map.entry(HOVER, 0x24FFFFFF),
            Map.entry(OVERLAY, 0xA0101114));

    private static final String[] SPRITE_KEYS = {
            SPRITE_PANEL, SPRITE_INSET, SPRITE_BUTTON, SPRITE_BUTTON_HOVER, SPRITE_BUTTON_PRESSED, SPRITE_BUTTON_DISABLED,
            SPRITE_TEXT_FIELD, SPRITE_TEXT_FIELD_FOCUSED, SPRITE_SCROLLBAR_TRACK, SPRITE_SCROLLBAR_THUMB,
            SPRITE_SCROLLBAR_THUMB_HOVER, SPRITE_TAB, SPRITE_TAB_SELECTED, SPRITE_SIDEBAR_HOVER, SPRITE_TOGGLE_OFF,
            SPRITE_TOGGLE_ON
    };

    private static final HibiscusTheme DEFAULT = new HibiscusTheme(DEFAULT_COLORS, defaultSprites());
    private static volatile HibiscusTheme current = DEFAULT;

    private final Map<String, Integer> colors;
    private final Map<String, Identifier> sprites;

    private HibiscusTheme(Map<String, Integer> colors, Map<String, Identifier> sprites) {
        this.colors = Map.copyOf(colors);
        this.sprites = Map.copyOf(sprites);
    }

    public static HibiscusTheme current() {
        return current;
    }

    public static HibiscusTheme builtIn() {
        return DEFAULT;
    }

    static void set(HibiscusTheme theme) {
        current = theme;
    }

    // A named colour (ARGB); an unknown name is magenta, so a typo in a theme or a caller shows at once.
    public int color(String name) {
        Integer color = colors.get(name);
        return color != null ? color : 0xFFFF00FF;
    }

    public Identifier sprite(String key) {
        Identifier sprite = sprites.get(key);
        return sprite != null ? sprite : HibiscusLib.id(key);
    }

    public int text() {
        return color(TEXT);
    }

    public int textMuted() {
        return color(TEXT_MUTED);
    }

    public int accent() {
        return color(ACCENT);
    }

    // Draws a sprite key's nine-slice stretched over the rectangle.
    public void draw(GuiGraphicsExtractor graphics, String spriteKey, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(spriteKey), x, y, width, height);
        }
    }

    public void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        draw(graphics, SPRITE_PANEL, x, y, width, height);
    }

    public void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        draw(graphics, SPRITE_INSET, x, y, width, height);
    }

    // A button background for its state.
    public void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean active, boolean hovered, boolean pressed) {
        String key = !active ? SPRITE_BUTTON_DISABLED : pressed ? SPRITE_BUTTON_PRESSED : hovered ? SPRITE_BUTTON_HOVER : SPRITE_BUTTON;
        draw(graphics, key, x, y, width, height);
    }

    // Reads a theme file over a base theme: keys the file leaves out keep the base's values.
    static HibiscusTheme parse(JsonObject json, HibiscusTheme base) {
        Map<String, Integer> colors = new HashMap<>(base.colors);
        Map<String, Identifier> sprites = new HashMap<>(base.sprites);
        if (json.has("colors")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("colors").entrySet()) {
                colors.put(entry.getKey(), parseColor(entry.getValue().getAsString()));
            }
        }
        if (json.has("sprites")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("sprites").entrySet()) {
                sprites.put(entry.getKey(), Identifier.parse(entry.getValue().getAsString()));
            }
        }
        return new HibiscusTheme(colors, sprites);
    }

    /**
     * Parses {@code #RRGGBB} (opaque) or {@code #AARRGGBB}.
     *
     * @throws IllegalArgumentException if it isn't one of those
     */
    public static int parseColor(String text) {
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("Not a #RRGGBB or #AARRGGBB colour: " + text);
        }
        int value = (int) Long.parseLong(hex.toLowerCase(Locale.ROOT), 16);
        return hex.length() == 6 ? 0xFF000000 | value : value;
    }

    private static Map<String, Identifier> defaultSprites() {
        Map<String, Identifier> sprites = new HashMap<>();
        for (String key : SPRITE_KEYS) {
            sprites.put(key, HibiscusLib.id(key));
        }
        return sprites;
    }
}
