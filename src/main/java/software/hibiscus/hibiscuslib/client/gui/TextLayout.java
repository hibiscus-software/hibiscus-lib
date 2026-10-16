/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Wrapped multi-paragraph text from a small markup, for quest descriptions, team info and help pages:
 *
 * <ul>
 *   <li>{@code # Heading} and {@code ## Subheading} lines;</li>
 *   <li>{@code - item} (or {@code * item}) bullet lines;</li>
 *   <li>{@code **bold**} and {@code *italic*} inside a line;</li>
 *   <li>Minecraft colour and format codes with {@code &} or {@code §} ({@code &c}, {@code &l}, {@code &r}...);</li>
 *   <li>links, {@code [label](https://example.com)}: clicking one asks before opening it, like chat links;</li>
 *   <li>{@code {item:minecraft:diamond}}: the item's icon inline, with its tooltip on hover;</li>
 *   <li>{@code {image:mymod:textures/gui/map.png 120 80}} on a line of its own: a picture, that many pixels wide and
 *       tall (scaled down to fit the width);</li>
 *   <li>a blank line between paragraphs.</li>
 * </ul>
 *
 * Callers can add their own {@code {name:argument}} tokens with {@link #parse(String, Tokens)}, for example a link that
 * opens a page of the caller's own screen (a {@link ClickEvent.Custom}, handled with
 * {@link software.hibiscus.hibiscuslib.client.gui.widget.TextLayoutWidget#onClickEvent}). A token nobody handles is
 * shown as written.
 *
 * Parse once; {@link #height(Font, int)}, {@link #draw} and {@link #styleAt} wrap to a width, caching the last one.
 */
public final class TextLayout {
    private static final int BULLET_INDENT = 10;
    private static final int PARAGRAPH_GAP = 4;
    private static final int HEADING_GAP = 3;
    private static final int IMAGE_GAP = 2;
    // Stands in for an inline item icon in the text: no-break spaces, so the icon is never split across lines.
    private static final String ICON_PLACEHOLDER = "   ";
    // Marks a placeholder's style (as its insertion text) with the index of its item.
    private static final String ICON_MARKER = "\u0000hibiscuslib:item:";

    /**
     * Resolves a caller's own {@code {name:argument}} tokens.
     */
    @FunctionalInterface
    public interface Tokens {
        Tokens NONE = (name, argument, style) -> null;

        /**
         * What a token shows, or null to leave it alone (the built-in tokens are tried next, then it's shown as
         * written).
         *
         * @param name     the part before the colon, e.g. {@code quest}
         * @param argument the rest, e.g. {@code basics/first_steps}
         * @param style    the style the text around the token has
         */
        @Nullable Component resolve(String name, String argument, Style style);
    }

    private enum Kind { TEXT, HEADING, SUBHEADING, BULLET, BLANK, IMAGE }

    private record Block(Kind kind, Component text, @Nullable Image image) {
        Block(Kind kind, Component text) {
            this(kind, text, null);
        }
    }

    private record Image(Identifier texture, int width, int height) {}

    // A wrapped line: where it sits and what's on it (or a picture).
    private record Line(int x, int y, FormattedText text, boolean bullet, @Nullable Image image, int imageWidth, int imageHeight) {
        Line(int x, int y, FormattedText text, boolean bullet) {
            this(x, y, text, bullet, null, 0, 0);
        }
    }

    // Parsing state: the caller's tokens and the items the text shows.
    private static final class Parse {
        final Tokens tokens;
        final List<ItemStack> items = new ArrayList<>();

        Parse(Tokens tokens) {
            this.tokens = tokens;
        }
    }

    private final List<Block> blocks;
    private final List<ItemStack> items;
    private @Nullable List<Line> lines;
    private int laidOutWidth = -1;
    private int laidOutHeight;

    private TextLayout(List<Block> blocks, List<ItemStack> items) {
        this.blocks = blocks;
        this.items = List.copyOf(items);
    }

    public static TextLayout parse(String markup) {
        return parse(markup, Tokens.NONE);
    }

    public static TextLayout parse(String markup, Tokens tokens) {
        HibiscusTheme theme = HibiscusTheme.current();
        Parse parse = new Parse(tokens);
        List<Block> blocks = new ArrayList<>();
        for (String raw : markup.replace("\r", "").split("\n", -1)) {
            String line = raw.strip();
            Image image = image(line);
            if (image != null) {
                blocks.add(new Block(Kind.IMAGE, Component.empty(), image));
            } else if (line.isEmpty()) {
                blocks.add(new Block(Kind.BLANK, Component.empty()));
            } else if (line.startsWith("## ")) {
                blocks.add(new Block(Kind.SUBHEADING, inline(line.substring(3), Style.EMPTY.withBold(true), parse)));
            } else if (line.startsWith("# ")) {
                blocks.add(new Block(Kind.HEADING, inline(line.substring(2), Style.EMPTY.withBold(true).withColor(theme.accent()), parse)));
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                blocks.add(new Block(Kind.BULLET, inline(line.substring(2), Style.EMPTY, parse)));
            } else {
                blocks.add(new Block(Kind.TEXT, inline(line, Style.EMPTY, parse)));
            }
        }
        return new TextLayout(blocks, parse.items);
    }

    // {image:<texture> <width> <height>} alone on a line.
    private static @Nullable Image image(String line) {
        if (!line.startsWith("{image:") || !line.endsWith("}")) {
            return null;
        }
        String[] parts = line.substring(7, line.length() - 1).trim().split("\\s+");
        if (parts.length != 3) {
            return null;
        }
        Identifier texture = Identifier.tryParse(parts[0]);
        try {
            int width = Integer.parseInt(parts[1]);
            int height = Integer.parseInt(parts[2]);
            return texture != null && width > 0 && height > 0 && width <= 1024 && height <= 1024 ? new Image(texture, width, height) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // One line's inline markup: **bold**, *italic*, [label](url), {tokens} and &/§ format codes.
    static MutableComponent inline(String text, Style base) {
        return inline(text, base, new Parse(Tokens.NONE));
    }

    private static MutableComponent inline(String text, Style base, Parse parse) {
        MutableComponent out = Component.empty();
        Style style = base;
        boolean bold = false;
        boolean italic = false;
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                flush(out, run, style);
                bold = !bold;
                style = style.withBold(bold || Boolean.TRUE.equals(base.isBold()) ? true : null);
                i += 2;
                continue;
            }
            // A single * starts italics only when another one closes them later on the line, so "2 * 3" stays as is.
            if (c == '*' && (italic || hasClosingStar(text, i + 1))) {
                flush(out, run, style);
                italic = !italic;
                style = style.withItalic(italic || Boolean.TRUE.equals(base.isItalic()) ? true : null);
                i++;
                continue;
            }
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
                if (format != null) {
                    flush(out, run, style);
                    style = format == ChatFormatting.RESET ? base : style.applyLegacyFormat(format);
                    i += 2;
                    continue;
                }
            }
            if (c == '{') {
                int end = text.indexOf('}', i);
                int colon = end < 0 ? -1 : text.indexOf(':', i);
                if (end > i && colon > i && colon < end) {
                    Component token = token(text.substring(i + 1, colon), text.substring(colon + 1, end), style, parse);
                    if (token != null) {
                        flush(out, run, style);
                        out.append(token);
                        i = end + 1;
                        continue;
                    }
                }
            }
            if (c == '[') {
                int close = text.indexOf("](", i);
                int end = close < 0 ? -1 : text.indexOf(')', close);
                if (close > i && end > close) {
                    String label = text.substring(i + 1, close);
                    String target = text.substring(close + 2, end).trim();
                    URI uri = linkTarget(target);
                    if (uri != null) {
                        flush(out, run, style);
                        Style link = style.withColor(HibiscusTheme.current().color(HibiscusTheme.ACCENT_HOVER)).withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(uri))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(uri.toString())));
                        out.append(Component.literal(label).setStyle(link));
                        i = end + 1;
                        continue;
                    }
                }
            }
            run.append(c);
            i++;
        }
        flush(out, run, style);
        return out;
    }

    private static boolean hasClosingStar(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '*') {
                // A ** pair belongs to bold; only a lone * closes italics.
                if (i + 1 < text.length() && text.charAt(i + 1) == '*') {
                    i++;
                    continue;
                }
                return i > from;
            }
        }
        return false;
    }

    // The caller's tokens first, then the built-in {item:...}.
    private static @Nullable Component token(String name, String argument, Style style, Parse parse) {
        Component custom = parse.tokens.resolve(name, argument.trim(), style);
        if (custom != null) {
            return custom;
        }
        if (name.equals("item")) {
            Identifier id = Identifier.tryParse(argument.trim());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                int index = parse.items.size();
                parse.items.add(new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                return Component.literal(ICON_PLACEHOLDER).setStyle(style.withInsertion(ICON_MARKER + index));
            }
        }
        return null;
    }

    private static void flush(MutableComponent out, StringBuilder run, Style style) {
        if (!run.isEmpty()) {
            out.append(Component.literal(run.toString()).setStyle(style));
            run.setLength(0);
        }
    }

    // Only web links: anything else (file:, javascript:) is left as plain text.
    private static @Nullable URI linkTarget(String target) {
        String lower = target.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            return null;
        }
        try {
            return new URI(target);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Line> layout(Font font, int width) {
        if (lines != null && laidOutWidth == width) {
            return lines;
        }
        List<Line> laidOut = new ArrayList<>();
        int y = 0;
        boolean first = true;
        for (Block block : blocks) {
            switch (block.kind()) {
                case BLANK -> y += PARAGRAPH_GAP;
                case HEADING, SUBHEADING -> {
                    if (!first) {
                        y += HEADING_GAP;
                    }
                    for (FormattedText part : font.getSplitter().splitLines(block.text(), width, Style.EMPTY)) {
                        laidOut.add(new Line(0, y, part, false));
                        y += font.lineHeight + 1;
                    }
                    y += 1;
                }
                case BULLET -> {
                    boolean firstPart = true;
                    for (FormattedText part : font.getSplitter().splitLines(block.text(), width - BULLET_INDENT, Style.EMPTY)) {
                        laidOut.add(new Line(BULLET_INDENT, y, part, firstPart));
                        firstPart = false;
                        y += font.lineHeight + 1;
                    }
                }
                case TEXT -> {
                    for (FormattedText part : font.getSplitter().splitLines(block.text(), width, Style.EMPTY)) {
                        laidOut.add(new Line(0, y, part, false));
                        y += font.lineHeight + 1;
                    }
                }
                case IMAGE -> {
                    Image image = block.image();
                    // Scaled down (never up) to fit the width, keeping its shape.
                    double scale = Math.min(1.0, (double) width / image.width());
                    int w = Math.max(1, (int) (image.width() * scale));
                    int h = Math.max(1, (int) (image.height() * scale));
                    y += first ? 0 : IMAGE_GAP;
                    laidOut.add(new Line(0, y, FormattedText.EMPTY, false, image, w, h));
                    y += h + IMAGE_GAP;
                }
            }
            first = false;
        }
        lines = laidOut;
        laidOutWidth = width;
        laidOutHeight = y;
        return laidOut;
    }

    public int height(Font font, int width) {
        layout(font, width);
        return laidOutHeight;
    }

    // Draws the text wrapped to width, in the theme's text colour where the markup doesn't say otherwise.
    public void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int color) {
        int muted = HibiscusTheme.current().textMuted();
        for (Line line : layout(font, width)) {
            if (line.image() != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, line.image().texture(), x + line.x(), y + line.y(), 0, 0,
                        line.imageWidth(), line.imageHeight(), line.imageWidth(), line.imageHeight());
                continue;
            }
            if (line.bullet()) {
                graphics.text(font, "•", x + 2, y + line.y(), muted, false);
            }
            graphics.text(font, Language.getInstance().getVisualOrder(line.text()), x + line.x(), y + line.y(), color, false);
            if (!items.isEmpty()) {
                drawIcons(graphics, font, line, x, y);
            }
        }
    }

    // The inline item icons on a line, drawn over their placeholders at text height.
    private void drawIcons(GuiGraphicsExtractor graphics, Font font, Line line, int x, int y) {
        double[] cursor = {line.x()};
        line.text().visit((style, text) -> {
            int segment = font.width(FormattedText.of(text, style));
            ItemStack item = itemOf(style);
            if (item != null) {
                float size = Math.min(segment, font.lineHeight + 1);
                graphics.pose().pushMatrix();
                graphics.pose().translate((float) (x + cursor[0] + (segment - size) / 2), y + line.y() - 1);
                graphics.pose().scale(size / 16f, size / 16f);
                graphics.item(item, 0, 0);
                graphics.pose().popMatrix();
            }
            cursor[0] += segment;
            return Optional.empty();
        }, Style.EMPTY);
    }

    private @Nullable ItemStack itemOf(Style style) {
        String insertion = style.getInsertion();
        if (insertion == null || !insertion.startsWith(ICON_MARKER)) {
            return null;
        }
        try {
            int index = Integer.parseInt(insertion.substring(ICON_MARKER.length()));
            return index >= 0 && index < items.size() ? items.get(index) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // The style under a point (relative to the text's top-left), for links and hover text.
    public Optional<Style> styleAt(Font font, int width, double relX, double relY) {
        for (Line line : layout(font, width)) {
            if (line.image() != null || relY < line.y() || relY >= line.y() + font.lineHeight) {
                continue;
            }
            double[] cursor = {line.x()};
            return line.text().visit((style, text) -> {
                int segment = font.width(FormattedText.of(text, style));
                if (relX >= cursor[0] && relX < cursor[0] + segment) {
                    return Optional.of(style);
                }
                cursor[0] += segment;
                return Optional.empty();
            }, Style.EMPTY);
        }
        return Optional.empty();
    }

    // The inline item icon under a point, for its tooltip.
    public Optional<ItemStack> itemAt(Font font, int width, double relX, double relY) {
        return styleAt(font, width, relX, relY).map(this::itemOf);
    }
}
