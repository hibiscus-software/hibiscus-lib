# Hibiscus Lib

The shared library every Hibiscus mod builds on (NeoForge, Minecraft 26.3, Java 25): the inventory button bar, the GUI
toolkit and theme, config/network/SNBT/permission/profile helpers, and the Teams API with its solo-team fallback.
Hibiscus Teams (`hibiscusteams`) implements the Teams API; Hibiscus Quests (`hibiscusquests`) stores progress per team
through it. Design public pieces for those two consumers.

- Clean room: FTB Library (https://github.com/FTBTeam/FTB-Library) is a behavioural reference only. Never copy its
  code, textures or lang text.
- Package `software.hibiscus.hibiscuslib`. The Teams API lives in its own source set, `src/api/java`
  (`software.hibiscus.hibiscuslib.api`), which can't see the mod's internals; it is also published alone as
  `hibiscuslib-api`. Once 1.0 ships, keep it backwards compatible: add default methods, never change signatures, and
  raise `HibiscusLibApi.API_VERSION` (see docs/API.md).
- Server-authoritative: every mutation goes client → payload → validated on the server (permission, rank, rate
  limit) → saved → synced. Clients never trust their own state for permission checks.
- Dedicated servers: no client classes on the server. Client code lives under `client/` and is only reached from
  `HibiscusLibClient` (`@Mod(dist = Dist.CLIENT)`) or behind `Dist.CLIENT` checks.
- Every user-facing string goes through `assets/hibiscuslib/lang/en_us.json`.
- Comments: a short comment above each class and non-obvious method saying what it's for, as in the code already.
- GUI style: the default theme is flat fills and 1 px bevels, no gradients or noise. Sprites come from
  `tools/gui_sprite_gen.py` (regenerate rather than hand-edit); its colours match
  `assets/hibiscuslib/hibiscus_theme/default.json`, so change both together.
- 26.3 names: `Identifier` (not ResourceLocation), `GuiGraphicsExtractor` with `extractRenderState` /
  `extractBackground`, `MouseButtonEvent` / `KeyEvent`, `minecraft.gui.setScreen(...)` / `minecraft.gui.screen()`,
  `InputConstants.MOUSE_BUTTON_LEFT` (= 1) and `MOUSE_BUTTON_RIGHT` (= 3). When unsure, grep the decompiled sources
  (unzip `build/neoForm/neoFormJoined26.3-1/steps/patchUserDev/outputs.jar`) or Arcforge.
- Tests: `./gradlew build runGameTestServer`. Gametests live in `gametest/` and are registered in
  `HibiscusLibGameTests`.
- Changes go under `## [Unreleased]` in CHANGELOG.md as they're made.
- Releasing: follow docs/RELEASING.md. A pushed `v<mod_version>+<minecraft_version>` tag (e.g. `v1.0.0+26.3`) runs
  `.github/workflows/release.yml`, which publishes the mod and API jars on GitHub, to GitHub Packages, and uploads the
  mod jar to CurseForge.
- Minecraft versions: one branch each; `main` is the newest, `mc/<major.minor>` the others. The same version number
  has the same changes on every branch: make a change on one branch and cherry-pick it to the others.
