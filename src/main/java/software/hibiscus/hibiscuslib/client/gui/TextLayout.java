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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Wrapped multi-paragraph text from a small markup, for quest descriptions, team info and help pages:
 *
 * <ul>
 *   <li>{@code # Heading} and {@code ## Subheading} lines;</li>
 *   <li>{@code - item} (or {@code * item}) bullet lines;</li>
 *   <li>{@code **bold**} inside a line;</li>
 *   <li>Minecraft colour and format codes with {@code &} or {@code §} ({@code &c}, {@code &l}, {@code &r}...);</li>
 *   <li>links, {@code [label](https://example.com)}: clicking one asks before opening it, like chat links;</li>
 *   <li>a blank line between paragraphs.</li>
 * </ul>
 *
 * Parse once with {@link #parse(String)}; {@link #height(Font, int)}, {@link #draw} and {@link #styleAt} wrap to a width,
 * caching the last one.
 */
public final class TextLayout {
    private static final int BULLET_INDENT = 10;
    private static final int PARAGRAPH_GAP = 4;
    private static final int HEADING_GAP = 3;

    private enum Kind { TEXT, HEADING, SUBHEADING, BULLET, BLANK }

    private record Block(Kind kind, Component text) {}

    // A wrapped line: where it sits and what's on it.
    private record Line(int x, int y, FormattedText text, boolean bullet) {}

    private final List<Block> blocks;
    private @Nullable List<Line> lines;
    private int laidOutWidth = -1;
    private int laidOutHeight;

    private TextLayout(List<Block> blocks) {
        this.blocks = blocks;
    }

    public static TextLayout parse(String markup) {
        HibiscusTheme theme = HibiscusTheme.current();
        List<Block> blocks = new ArrayList<>();
        for (String raw : markup.replace("\r", "").split("\n", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) {
                blocks.add(new Block(Kind.BLANK, Component.empty()));
            } else if (line.startsWith("## ")) {
                blocks.add(new Block(Kind.SUBHEADING, inline(line.substring(3), Style.EMPTY.withBold(true))));
            } else if (line.startsWith("# ")) {
                blocks.add(new Block(Kind.HEADING, inline(line.substring(2), Style.EMPTY.withBold(true).withColor(theme.accent()))));
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                blocks.add(new Block(Kind.BULLET, inline(line.substring(2), Style.EMPTY)));
            } else {
                blocks.add(new Block(Kind.TEXT, inline(line, Style.EMPTY)));
            }
        }
        return new TextLayout(blocks);
    }

    // One line's inline markup: **bold**, [label](url) and &/§ format codes.
    static MutableComponent inline(String text, Style base) {
        MutableComponent out = Component.empty();
        Style style = base;
        boolean bold = false;
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
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
                if (format != null) {
                    flush(out, run, style);
                    style = format == ChatFormatting.RESET ? base : style.applyLegacyFormat(format);
                    i += 2;
                    continue;
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
            if (line.bullet()) {
                graphics.text(font, "•", x + 2, y + line.y(), muted, false);
            }
            graphics.text(font, Language.getInstance().getVisualOrder(line.text()), x + line.x(), y + line.y(), color, false);
        }
    }

    // The style under a point (relative to the text's top-left), for links and hover text.
    public Optional<Style> styleAt(Font font, int width, double relX, double relY) {
        for (Line line : layout(font, width)) {
            if (relY < line.y() || relY >= line.y() + font.lineHeight) {
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
}
