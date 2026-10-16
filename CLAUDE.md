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
- NeoForge range: the jar declares every 26.3 build from 26.3.0.0-beta (`neo_version_range`) while compiling against
  `neo_version`. To check an older build, run the already compiled classes on it:
  `./gradlew runGameTestServer -x compileJava -x compileApiJava -Pneo_version=26.3.0.0-beta -Pno_jei` (add
  `runScreenshotClient` for GUI changes). Something that differs between builds is looked up by name, as Encoded
  Logistics does for `ModConfig.Type.LOCAL`.
- Tests: `./gradlew build runGameTestServer`. Gametests live in `gametest/` and are registered in
  `HibiscusLibGameTests`.
- GUI checks: `./gradlew runScreenshotClient` (source set `src/screenshots`, never in a jar) opens a flat creative
  world, screenshots the button bar, edit screen and widget gallery at GUI scales 1-4 into
  `run/screenshotClient/screenshots/`, and quits. Look at them after any GUI change; the README's images come from
  there (`docs/images/`).
- Item stacks can't be made during mod construction or client setup in 26.3 (data components aren't bound yet):
  give icons a supplier and make the stack on first use.
- Changes go under `## [Unreleased]` in CHANGELOG.md as they're made.
- Consumers: docs/DEPENDING.md has the Gradle setup (GitHub Packages, includeBuild, mavenLocal); docs/API.md the
  Teams API and toolkit. Keep both current when public pieces change.
- Releasing: follow docs/RELEASING.md. A pushed `v<mod_version>+<minecraft_version>` tag (e.g. `v1.0.0+26.3`) runs
  `.github/workflows/release.yml`, which publishes the mod and API jars on GitHub, to GitHub Packages, and uploads the
  mod jar to CurseForge.
- Minecraft versions: one branch each; `main` is the newest, `mc/<major.minor>` the others. The same version number
  has the same changes on every branch: make a change on one branch and cherry-pick it to the others.
