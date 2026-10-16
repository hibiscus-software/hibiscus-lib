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

Suggested version: **1.1.1** (a fix; the API is unchanged).

### Fixed

- Two of the Teams API game tests (`teams_fallback_installed`, `teams_extra_data_persists`) failed when a teams mod
  was installed, as in Hibiscus Teams' dev runs, where Hibiscus Lib's tests run alongside its own. The fallback tests
  now test the fallback directly, and `teams_fallback_installed` accepts a teams mod that has installed itself.

## [1.1.0] - 2026-10-15

### Added

- `ConfirmDialog.requireTyping(text)`: for actions that can't be undone, the confirm button stays disabled until the
  player types the given text (such as the name of the team being disbanded).
- `Icon.playerFace(uuid)`: a player's face from their skin, for offline players too (the default skin until theirs
  loads), and `Icon.withDot(color)` to mark an icon, for example with an online dot.
- Both are in the widget gallery (`/hibiscuslib widgets`): a type-to-confirm dialog on the Popups tab and player
  faces on the Lists tab.

### Fixed

- Disabled themed buttons drew their label in vanilla's grey instead of the theme's disabled colour.

## [1.0.0] - 2026-10-15

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
  - When there isn't room for more columns (a narrow window, or the recipe book open), columns grow taller instead,
    clear of the recipe book's tabs and JEI's corner buttons.
- **GUI toolkit and theme**, so every Hibiscus screen looks like one family and a resource pack can restyle them all.
  - `HibiscusTheme`: named colours (`text`, `text_muted`, `accent`, `positive`, `warning`, `negative`, `gold` and
    more) and nine-slice sprites for panels, insets, buttons, text fields, scrollbars, tabs and toggles, from
    `assets/hibiscuslib/hibiscus_theme/default.json`. A resource pack's copy only needs the keys it changes.
  - The default theme is a neutral dark UI with a hibiscus coral accent: flat fills and 1 px bevels.
  - Widgets: `Panel`, `ScrollPanel`, `ListWidget`, `TextButton`, `IconButton`, `ToggleButton`, `TextInput` (with a
    placeholder and validation), `ColorPicker`, `ItemSlotWidget`, `Tabs` and `TextLayoutWidget`. All work with the
    keyboard (Tab, arrows, Enter) and the narrator.
  - `BaseScreen`: a centred panel that shrinks to fit (GUI scale 1 to 4 from 1280×720 up), modal popups
    (`ConfirmDialog`, `ContextMenu`), and the same close behaviour everywhere (Esc, or right-click outside).
  - `TextLayout`: wrapped text with `# headings`, `- bullets`, `**bold**`, colour codes and links that ask before
    opening.
  - `HibiscusToasts` for themed toasts, and `RecipeViewers.showRecipes` / `showUses`, which open JEI (or EMI) and do
    nothing when neither is installed. JEI's keys work over item slots in Hibiscus screens.
  - `/hibiscuslib widgets` opens a gallery of every widget. It's a debug command: there in a development
    environment, or with `debug.widgetGallery` on in `hibiscuslib-client.toml`.

- **Teams API** (`software.hibiscus.hibiscuslib.api.team`, also published alone as `hibiscuslib-api`), for team mods
  to provide and other mods to use, with or without a teams mod installed. See [docs/API.md](docs/API.md).
  - `TeamsApi.get()` returns the installed teams mod's teams, or a fallback where every player is their own team (id =
    the player's UUID), so per-team data works the same either way.
  - `Team`, `TeamRank` (`ENEMY` up to `OWNER`), `TeamType`, `TeamManager`, and `ClientTeams` for screens.
  - Per-team storage for other mods (`Team.extraData`), saved with the team; under the fallback in
    `data/hibiscuslib/solo_teams.dat`.
  - Events: `TeamCreatedEvent`, `TeamDeletedEvent`, `PlayerJoinTeamRequestEvent` (cancellable), `PlayerLeftTeamEvent`,
    `PlayerJoinedTeamEvent` (with the team the player came from), `TeamPropertiesChangedEvent` and
    `TeamAlliesChangedEvent`.
- **Helpers** for mods built on Hibiscus Lib:
  - `HibiscusNetwork`: payload registration for a mod's channel, and sending to a player, a team's online members or
    everyone.
  - `SnbtFiles`: SNBT files under `config/`, written atomically with a `.bak` of the previous version.
  - `PermissionLevels`: permission nodes that default to a vanilla op level, so permission mods can manage them.
  - `Profiles`: player name ↔ UUID lookups that work for offline players.
  - `RateLimiter`: per-player limits for client requests.

[Unreleased]: https://github.com/hibiscus-software/hibiscus-lib/compare/v1.1.0+26.3...HEAD
[1.1.0]: https://github.com/hibiscus-software/hibiscus-lib/compare/v1.0.0+26.3...v1.1.0+26.3
[1.0.0]: https://github.com/hibiscus-software/hibiscus-lib/commits/v1.0.0+26.3
