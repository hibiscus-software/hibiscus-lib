# Changelog

All notable changes to Hibiscus Lib are listed here, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Versioning

Versions are `MAJOR.MINOR.PATCH+MINECRAFT` (for example `1.0.0+26.3`), set by `mod_version` and `minecraft_version` in
`gradle.properties`, and each is released as the tag `vMAJOR.MINOR.PATCH+MINECRAFT`. Each Minecraft version has its own
branch and its own changelog; this one is Minecraft 26.3's (`main`). The same version number on two Minecraft versions
has the same changes.

- **PATCH** (1.0.0 → 1.0.1): bug fixes and small tweaks. Configs and saved data carry over untouched, and no public
  type or method changes.
- **MINOR** (1.0.0 → 1.1.0): new features, widgets or API additions (new default methods, new types). Mods built
  against an older 1.x keep working.
- **MAJOR** (1.x → 2.0.0): anything that breaks mods built on Hibiscus Lib or the data it saves.

The Teams API (`hibiscuslib-api`) has its own version, `HibiscusLibApi.API_VERSION`, with the same rules; see
[docs/API.md](docs/API.md).

Changes land under **Unreleased** as they're made; when a version is released, that section gets its number and
date. How to cut a release is in [docs/RELEASING.md](docs/RELEASING.md).

## [Unreleased]

Suggested version: **1.0.0** (first release).

### Added

- **Inventory button bar.** A column of 16×16 icon buttons beside the survival and creative inventories, the entry
  point for every Hibiscus mod. It follows the recipe book as it opens and closes, wraps into another column when it
  runs out of room, and JEI keeps its item list and bookmarks clear of it.
  - Mods register buttons with `HibiscusButtons.register(id, ButtonSpec)`: an icon (a GUI sprite or an item), a
    tooltip, a click action, an optional visibility check, an optional corner badge (a count or a dot) and a default
    order.
  - Resource packs and modpacks add buttons with `assets/<ns>/hibiscus_buttons/<id>.json`, which run a command or an
    action a mod registered.
  - Buttons are real widgets: Tab moves between them, Enter or Space presses them and the narrator reads them.
  - Each button can have its own key mapping (unbound by default) in a new **Hibiscus** controls category.
  - Right-click any button to edit the bar: tick which buttons to show and drag them into order.
  - `hibiscuslib-client.toml`: `buttons.enabled`, `buttons.location` (`top_left`, `top_right`, `bottom_left`,
    `left_column`), `buttons.hidden` and `buttons.order`.
