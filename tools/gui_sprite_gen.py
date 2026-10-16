#!/usr/bin/env python3
# Generates Hibiscus Lib's default GUI sprites (16x16 nine-slices and a few icons) under
# src/main/resources/assets/hibiscuslib/textures/gui/sprites/, with their .mcmeta scaling files.
# Style: flat fills, 1 px bevels (light top-left, dark bottom-right), no gradients or noise. The colours are the default
# theme's (assets/hibiscuslib/hibiscus_theme/default.json); change both together.
# Usage: python tools/gui_sprite_gen.py
import json
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hibiscuslib", "textures", "gui", "sprites")

PANEL = "#2B2D31"
HIGHLIGHT = "#3F4248"
SHADOW = "#18191C"
RECESS = "#1E1F22"
BUTTON = "#383A40"
BUTTON_HOVER = "#45474E"
BUTTON_PRESSED = "#26282C"
DISABLED = "#232428"
DISABLED_EDGE = "#2E3035"
ACCENT = "#F2557A"
ACCENT_HOVER = "#FF7898"
TEXT_MUTED = "#9A9DA3"
TEXT = "#E6E6E6"


def rgba(hex_color, alpha=255):
    h = hex_color.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha)


def bevel(fill, light, dark, size=16, outline=None):
    """A flat box: fill, 1 px light top/left edge, 1 px dark bottom/right edge, optional 1 px outline outside it."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    inset = 1 if outline else 0
    for y in range(size):
        for x in range(size):
            if outline and (x == 0 or y == 0 or x == size - 1 or y == size - 1):
                px[x, y] = rgba(outline)
                continue
            lo, hi = inset, size - 1 - inset
            if y == lo or x == lo:
                px[x, y] = rgba(light)
            elif y == hi or x == hi:
                px[x, y] = rgba(dark)
            else:
                px[x, y] = rgba(fill)
    if outline:
        # Leave the outline's corners empty, for a slightly rounded look.
        for (x, y) in ((0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)):
            px[x, y] = (0, 0, 0, 0)
    return img


def save(name, img, border=None, width=16, height=16, stretch=False):
    path = os.path.join(OUT, name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    if border is not None:
        scaling = {"type": "nine_slice", "width": width, "height": height, "border": border}
        if stretch:
            scaling["stretch_inner"] = True
        with open(path + ".mcmeta", "w", encoding="utf-8") as f:
            json.dump({"gui": {"scaling": scaling}}, f, indent=2)
            f.write("\n")


def toggle(on):
    """A 22x12 switch: a track with a knob. Only the track's colour and the knob's side change between off and on;
    the knob is drawn the same way in both, so the two read as one control."""
    img = Image.new("RGBA", (22, 12), (0, 0, 0, 0))
    px = img.load()
    track = ACCENT if on else RECESS
    for y in range(12):
        for x in range(22):
            corner = (x in (0, 21)) and (y in (0, 11))
            if corner:
                continue
            edge = x in (0, 21) or y in (0, 11)
            px[x, y] = rgba(SHADOW if edge else track)
    kx = 11 if on else 1
    for y in range(1, 11):
        for x in range(kx, kx + 10):
            if x == kx or y == 1:
                px[x, y] = rgba("#FFFFFF")
            elif x == kx + 9 or y == 10:
                px[x, y] = rgba(TEXT_MUTED)
            else:
                px[x, y] = rgba(TEXT)
    return img


def sidebar_hover():
    """An 18x18 rounded frame drawn behind a hovered sidebar button: translucent fill and an accent edge."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    for y in range(18):
        for x in range(18):
            corner = (x in (0, 17)) and (y in (0, 17))
            if corner:
                continue
            edge = x in (0, 17) or y in (0, 17)
            px[x, y] = rgba(ACCENT, 255) if edge else (255, 255, 255, 40)
    return img


def tab(selected):
    """A 16x16 tab: open at the bottom, so it joins the panel below it when selected."""
    fill = PANEL if selected else BUTTON_PRESSED
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x in (0, 15)) and y == 0:
                continue
            if y == 0 or x == 0:
                px[x, y] = rgba(HIGHLIGHT if selected else PANEL)
            elif x == 15:
                px[x, y] = rgba(SHADOW)
            else:
                px[x, y] = rgba(fill)
    if selected:
        for x in range(2, 14):
            px[x, 1] = rgba(ACCENT)
    return img


def icon_gear():
    """A 16x16 settings icon, for the gallery's demo sidebar button."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = (dx * dx + dy * dy) ** 0.5
            tooth = (abs(dx) < 1.6 or abs(dy) < 1.6 or abs(abs(dx) - abs(dy)) < 1.2)
            if r <= 2.2:
                continue
            if r <= 5.2 or (r <= 7.4 and tooth):
                px[x, y] = rgba(TEXT if r <= 5.2 else TEXT_MUTED)
    return img


def icon_flower():
    """A 16x16 hibiscus mark, Hibiscus Lib's own icon."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    import math
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            petal = 5.0 + 2.4 * math.cos(5 * a)
            if r <= 1.6:
                px[x, y] = rgba("#F2C14E")
            elif r <= petal:
                px[x, y] = rgba(ACCENT_HOVER if r < petal - 1.5 else ACCENT)
    return img


def main():
    save("panel", bevel(PANEL, HIGHLIGHT, SHADOW), border=2)
    save("inset", bevel(RECESS, SHADOW, HIGHLIGHT), border=2)
    save("button", bevel(BUTTON, HIGHLIGHT, SHADOW), border=2)
    save("button_hover", bevel(BUTTON_HOVER, ACCENT_HOVER, SHADOW), border=2)
    save("button_pressed", bevel(BUTTON_PRESSED, SHADOW, HIGHLIGHT), border=2)
    save("button_disabled", bevel(DISABLED, DISABLED_EDGE, DISABLED_EDGE), border=2)
    save("text_field", bevel(RECESS, SHADOW, HIGHLIGHT), border=2)
    save("text_field_focused", bevel(RECESS, ACCENT, ACCENT), border=2)
    save("scrollbar_track", bevel(RECESS, SHADOW, SHADOW), border=2)
    save("scrollbar_thumb", bevel(BUTTON_HOVER, HIGHLIGHT, SHADOW), border=2)
    save("scrollbar_thumb_hover", bevel(ACCENT, ACCENT_HOVER, SHADOW), border=2)
    save("tab", tab(False), border=3)
    save("tab_selected", tab(True), border=3)
    save("sidebar_hover", sidebar_hover(), border=2, width=18, height=18)
    save("toggle_off", toggle(False))
    save("toggle_on", toggle(True))
    save("icon/settings", icon_gear())
    save("icon/hibiscus", icon_flower())


if __name__ == "__main__":
    main()
