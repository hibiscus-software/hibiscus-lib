# Releasing

A release is a pushed `vX.Y.Z+MC` tag, where `X.Y.Z` is `mod_version` and `MC` is `minecraft_version` in
`gradle.properties` (for example `v1.0.0+26.3`). `.github/workflows/release.yml` builds that commit, runs the game
tests and then:

1. publishes a GitHub Release, "Hibiscus Lib X.Y.Z for Minecraft MC", with two jars and the version's section of
   `CHANGELOG.md` as its notes;
2. publishes both jars, with sources (and the API's Javadoc), to **GitHub Packages**, the Maven repository Hibiscus
   Teams and Hibiscus Quests depend on (see [DEPENDING.md](DEPENDING.md));
3. in its `curseforge` job, uploads the mod jar and the same notes to CurseForge.

`build.yml` runs the same build and game tests on every push, so a release shouldn't be the first place a failure
shows up.

## Minecraft versions

Each supported Minecraft version has its own branch, and releases for it are tagged from that branch:

| Branch | Minecraft | Tags |
|---|---|---|
| `main` | 26.3 (the newest) | `vX.Y.Z+26.3` |

- `main` is always the newest Minecraft version; every other supported version gets a branch `mc/<major.minor>`
  when `main` moves on.
- Each branch has its own `gradle.properties` (Minecraft, NeoForge and JEI versions), its own `CHANGELOG.md` and its
  own code for whatever the Minecraft versions do differently.
- A version number means the same changes on every Minecraft version: 1.1.0 for one Minecraft version has what 1.1.0
  for another has. A version that only ships on some branches simply isn't released on the others.
- Make a change on the branch it's for (usually `main`), then carry it to the others with `git cherry-pick`, fixing
  whatever the other Minecraft version needs. Keep version-specific code small and in one place.
- Only `main`'s full releases are marked "Latest" on GitHub.
- Hibiscus Teams and Hibiscus Quests follow the same branches: their `mc/26.3` depends on Hibiscus Lib's `+26.3`
  builds.

## The two jars

- `hibiscuslib-X.Y.Z+MC.jar` is the mod, versioned by `mod_version`. Players install it (Hibiscus Teams and Hibiscus
  Quests require it). It goes on the GitHub Release, GitHub Packages and CurseForge.
- `hibiscuslib-api-A.B.C+MC.jar` is the Teams API on its own (see [API.md](API.md)), versioned by
  `HibiscusLibApi.API_VERSION` plus the Minecraft version. Mods that only need the Teams API compile against it; players
  don't install it, since the mod jar contains its classes. It goes on the GitHub Release and GitHub Packages, never on
  CurseForge.

If the API changed, raise `API_VERSION` (and its `MAJOR` / `MINOR` / `PATCH` constants) before the release, following
the API's own semantic versioning; if it didn't, the release attaches the same API version as the last one. Once
1.0.0 is out the API only grows: new default methods and types are a minor bump, and nothing is changed or removed
without a major one.

## Cutting a release

Do this on the branch for the Minecraft version you're releasing.

1. Pick the version from the rules at the top of `CHANGELOG.md` (the Unreleased section suggests one).
2. In `CHANGELOG.md`:
   - rename `## [Unreleased]` to `## [X.Y.Z] - YYYY-MM-DD` and delete its "Suggested version" line;
   - add a new, empty `## [Unreleased]` above it;
   - at the bottom, point `[Unreleased]` at `https://github.com/hibiscus-software/hibiscuslib/compare/vX.Y.Z+MC...HEAD`,
     and add `[X.Y.Z]: https://github.com/hibiscus-software/hibiscuslib/compare/vPREVIOUS...vX.Y.Z+MC`, where
     `vPREVIOUS` is the previous tag on this branch (for the branch's first release,
     `[X.Y.Z]: https://github.com/hibiscus-software/hibiscuslib/commits/vX.Y.Z+MC`).
3. Set `mod_version=X.Y.Z` in `gradle.properties`.
4. Check it all before tagging:

   ```sh
   bash .github/scripts/release-notes.sh X.Y.Z MC   # the same checks release.yml runs; writes release-notes.md
   ./gradlew build runGameTestServer
   ./gradlew runScreenshotClient                   # optional: look over the GUI at every scale
   ```

   `build/libs` should then have `hibiscuslib-X.Y.Z+MC.jar` and `hibiscuslib-api-A.B.C+MC.jar`.

5. Commit ("Release X.Y.Z for Minecraft MC"), then tag and push the branch and the tag:

   ```sh
   git tag -a vX.Y.Z+MC -m "Hibiscus Lib X.Y.Z for Minecraft MC"
   git push origin <branch> vX.Y.Z+MC
   ```

6. Watch the Release run on the Actions tab. When it's green, the release is on the Releases page with both jars, the
   packages are under the repository's Packages, and (once CurseForge has approved it) the mod jar is on CurseForge.

A tag with a pre-release part (`v1.1.0-beta.1+26.3`, with `mod_version=1.1.0-beta.1`) is published as a pre-release
(and a beta file on CurseForge).

## GitHub Packages

The `Publish to GitHub Packages` step runs `./gradlew publishAllPublicationsToGitHubPackagesRepository` with the
workflow's own `GITHUB_TOKEN` (the workflow has `packages: write`), so it needs no secret. It publishes to
`https://maven.pkg.github.com/hibiscus-software/hibiscuslib`:

- `software.hibiscus.hibiscuslib:hibiscuslib:X.Y.Z+MC` (with `-sources`)
- `software.hibiscus.hibiscuslib:hibiscuslib-api:A.B.C+MC` (with `-sources` and `-javadoc`)

The POMs list no dependencies (consumers have Minecraft and NeoForge already) and there's no Gradle module metadata.

For Hibiscus Teams' and Hibiscus Quests' workflows to download the packages with their own `GITHUB_TOKEN`, give those
repositories read access once: the package's page → **Package settings** → **Manage Actions access** → add
`hibiscusteams` and `hibiscusquests` with the Read role. Do it for both packages after the first release.

GitHub Packages never replaces a published version. If a release run fails after this step, publish the fix as a new
version rather than moving the tag; if it failed before it, see below.

## CurseForge

The upload is `.github/scripts/curseforge_upload.py`. It sends the mod jar and the version's changelog section, the
display name "Hibiscus Lib X.Y.Z+MC", the game versions (the branch's Minecraft version, NeoForge, Java 25, Client and
Server), `jei` as an optional dependency, and the file type: release, or beta / alpha for a pre-release.

It needs:

- the project's numeric id in the `CURSEFORGE_PROJECT_ID` repository **variable** (Settings → Secrets and variables →
  Actions → Variables). Until it's set the `curseforge` job is skipped, so releases work before the CurseForge project
  exists;
- a CurseForge upload API token in the `CURSEFORGE_TOKEN` repository **secret**. Make the token at
  authors.curseforge.com (account settings, API tokens), then run
  `gh secret set CURSEFORGE_TOKEN -R hibiscus-software/hibiscuslib` and paste it in. Don't commit it or put it
  anywhere else.

To check the metadata without uploading, run the script with `--dry-run` and the token in `CURSEFORGE_TOKEN` (the
arguments are in release.yml and at the top of the script).

## If the release run fails

If only the `curseforge` job failed (a bad token, CurseForge down, an unknown game version), everything else is
already out: fix the cause and use **Re-run failed jobs** on the run.

If the run failed before **Publish GitHub Release**, the tag is pushed but nothing is published. Fix the problem,
commit, then move the tag:

```sh
git push origin :refs/tags/vX.Y.Z+MC
git tag -f -a vX.Y.Z+MC -m "Hibiscus Lib X.Y.Z for Minecraft MC"
git push origin vX.Y.Z+MC
```

If it failed after the GitHub Release but before or during **Publish to GitHub Packages**, run that step's command
locally against the tagged commit (with a token that has `write:packages` in `GITHUB_TOKEN` and your user name in
`GITHUB_ACTOR`), or delete the release and tag and run it again.
