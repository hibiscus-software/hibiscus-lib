# Hibiscus Lib API

Hibiscus Lib has two parts mods build on:

- **The Teams API** (`software.hibiscus.hibiscuslib.api`, published on its own as `hibiscuslib-api`): stable,
  semantically versioned, documented with Javadoc on every type and method.
- **The toolkit** (the rest of `software.hibiscus.hibiscuslib`): the inventory button bar, GUI widgets and theme, and
  the network, SNBT, permission and profile helpers. It ships in the mod jar; compile against `hibiscuslib` itself to
  use it. How to add the dependency is in [DEPENDING.md](DEPENDING.md).

## Teams API

```java
Team team = TeamsApi.get().teamOf(player);        // on the server: always a team, never empty
CompoundTag data = team.extraData(MY_DATA_ID);    // your mod's storage on that team
data.putInt("completed", 3);
team.markDirty();                                 // saved with the world
```

`TeamsApi.get()` returns the installed teams mod's `TeamManager` (Hibiscus Teams), or Hibiscus Lib's **fallback**
when there is none. In the fallback every player is their own team:

| | Fallback (no teams mod) | With Hibiscus Teams |
|---|---|---|
| `teamOf(player)` | the player's solo team | their party, else their solo team |
| team id | the player's UUID | the party's id; a solo team's is still the player's UUID |
| `type()` | `PLAYER` | `PLAYER`, `PARTY` or `SERVER` |
| `members()` | `{player: OWNER}` | everyone in the party with their ranks |
| `allies()` | empty | allied teams |
| `extraData(id)` | saved in `data/hibiscuslib/solo_teams.dat` | saved with the team by Hibiscus Teams |
| events | `TeamCreatedEvent` on a player's first join | all of them |

So keying data by `teamOf(player).id()` works unchanged with or without Hibiscus Teams.

### Types

- `Team`: `id`, `type`, `displayName`, `shortName`, `color` (`0xRRGGBB`), `owner`, `members` (rank `MEMBER` and
  up only), `allies`, `rankOf(player)`, `isMember`, `isAlly(teamId)` (a team is allied with itself),
  `onlineMembers(server)`, `extraData(owningMod)` and `markDirty()`.
- `TeamRank`: `ENEMY < NONE < ALLY < INVITED < MEMBER < OFFICER < OWNER`, with `isAtLeast(rank)` and `isMember()`.
- `TeamType`: `PLAYER`, `PARTY`, `SERVER`.
- `TeamManager` (server): `teamOf(uuid)` / `teamOf(player)`, `team(teamId)`, `allTeams()`, `playerTeam(uuid)` (the
  solo team even while in a party) and `sameTeam(a, b)`.
- `ClientTeams` (client, for screens): `myTeam()`, `team(teamId)`, `knownTeams()`. Read-only and only as fresh as the
  last sync. Never use it to decide what a player may do; the server decides.

### Events

Posted on `NeoForge.EVENT_BUS`, on the server thread, by the teams mod:

| Event | When |
|---|---|
| `TeamCreatedEvent` | a team was created (a solo team: the player's first join) |
| `TeamDeletedEvent` | a party is about to be deleted; its `extraData` is still readable |
| `PlayerJoinTeamRequestEvent` | before a player joins; cancel it (`deny(reason)`) to stop them |
| `PlayerLeftTeamEvent` | a player left a party (`LEFT`, `KICKED`, `DELETED`, `SWITCHED`) |
| `PlayerJoinedTeamEvent` | a player's current team changed, with `from()` and `to()` |
| `TeamPropertiesChangedEvent` | name, colour, owner, ranks or other settings changed |
| `TeamAlliesChangedEvent` | allies added or removed |

**`PlayerJoinedTeamEvent` is the one to watch for per-team data.** It fires whenever `teamOf(player)` changes: joining
a party (`from` is the player's solo team), switching parties, and going back to the solo team after leaving, being
kicked or the party being deleted. Leaving fires `PlayerLeftTeamEvent` first, then `PlayerJoinedTeamEvent` to the solo
team. With both teams in hand a mod can keep its data where it is, merge `from`'s into `to`'s, or reset it:

```java
@SubscribeEvent
static void onTeamChange(PlayerJoinedTeamEvent event) {
    CompoundTag from = event.from().extraData(PROGRESS);
    CompoundTag to = event.to().extraData(PROGRESS);
    switch (QuestsConfig.ON_TEAM_CHANGE.get()) {
        case MERGE -> mergeInto(to, from);
        case RESET -> resetProgress(to);
        case KEEP -> {}
    }
    event.to().markDirty();
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    HibiscusNetwork.sendToTeam(server, event.to(), syncPayload(event.to()));
}
```

### Implementing it (Hibiscus Teams)

A teams mod calls `TeamsApi.install(manager, MODID)` from its mod constructor and, on the client,
`ClientTeams.install(provider)`. Only one mod can install; a second throws. It must:

- return a team from `teamOf` for every UUID (the player's solo team when they're in no party), with the player's
  UUID as the solo team's id, so data stored under the fallback still lines up;
- keep `extraData` per owning mod, saved with the team;
- post the events above, `PlayerJoinedTeamEvent` after every change of `teamOf`.

Data saved under the fallback before the teams mod was installed is in `SoloTeamStore.get(server).entries()`
(`software.hibiscus.hibiscuslib.team`): each player's name and per-mod tags, ready to copy into their player team.

### Versioning

`HibiscusLibApi.API_VERSION` follows semantic versioning on its own, separately from the mod: a patch for docs and
fixes, a minor for additions (new types, new **default** methods on interfaces, new enum constants), a major for any
change or removal. From 1.0 the API only grows: add default methods, never change a signature.

## Toolkit at a glance

| Area | Classes |
|---|---|
| Button bar | `HibiscusButtons.register(id, ButtonSpec)`, `ButtonSpec.builder()`, `ButtonBadge`, data files in `assets/<ns>/hibiscus_buttons/` |
| Screens | `BaseScreen` (centred panel, popups, close behaviour), `Popup`, `ConfirmDialog` (`.dangerous()`, `.requireTyping(text)` for type-to-confirm), `ContextMenu` |
| Widgets | `Panel`, `ScrollPanel`, `ListWidget<T>` (`.onContextMenu(...)` for right-click menus), `TextButton`, `IconButton`, `ToggleButton`, `TextInput`, `TextArea` (several lines), `ColorPicker`, `ItemSlotWidget`, `Tabs`, `TextLayoutWidget` |
| Theme | `HibiscusTheme.current()`: `color(name)`, `panel(...)`, `inset(...)`, `button(...)`, `draw(...)` |
| Text | `TextLayout.parse(markup)` (`# headings`, `- bullets`, `**bold**`, `*italic*`, `&` colour codes, `[links](https://...)`, `{item:id}` inline icons, `{image:texture w h}` pictures), `TextLayout.parse(markup, tokens)` for your own `{name:arg}` tokens, `TextLayoutWidget.onClickEvent(...)` for their clicks |
| Other client | `HibiscusToasts.show(...)`, `RecipeViewers.showRecipes/showUses(stack)`, `Icon` (`sprite`, `item`, `color`, `playerFace(uuid)`, `.withDot(color)`), `HibiscusClientNetwork.sendToServer` |
| Network | `HibiscusNetwork.channel(bus, version, channel -> ...)`, `sendToPlayer`, `sendToTeam`, `sendToAll` |
| Files | `SnbtFiles.read/write` (atomic, with `.bak`), `SnbtFiles.configFile(...)` |
| Permissions | `PermissionLevels.node(modId, name, defaultLevel)`, `check`, `hasLevel`, `requires(node, level)` for commands |
| Players | `Profiles.nameOf`, `idOf`, `idOrOffline`, `displayName`, working for offline players through the name cache |
| Requests | `RateLimiter` for client payloads |

Things to know:

- Register sidebar buttons that want a key mapping during mod construction; key mappings are collected before
  `FMLClientSetupEvent`.
- Don't build `ItemStack`s during mod construction or setup (26.3 hasn't bound data components yet): pass
  `ButtonSpec.item(() -> new ItemStack(...))` a supplier.
- Every client → server payload is untrusted: check permission (`PermissionLevels`), rank (`team.rankOf(sender)`) and
  rate (`RateLimiter`) in the handler, then save and sync.
- Open the gallery with `/hibiscuslib widgets` (in a dev environment, or with `debug.widgetGallery = true`) to see every
  widget in the current theme.
