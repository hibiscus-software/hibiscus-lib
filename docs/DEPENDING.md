# Depending on Hibiscus Lib

How Hibiscus Teams, Hibiscus Quests (and any other mod) build against Hibiscus Lib. All three use NeoGradle userdev,
Java 25 and the same Minecraft version branches (see [RELEASING.md](RELEASING.md)).

## From GitHub Packages (normal builds and CI)

Releases are published to `https://maven.pkg.github.com/hibiscus-software/hibiscus-lib`. GitHub Packages asks for a
token even to read public packages.

`gradle.properties` (the consumer's):

```properties
hibiscuslib_version=1.0.0+26.3
```

`build.gradle`:

```groovy
repositories {
    maven {
        name = 'HibiscusLib'
        url = 'https://maven.pkg.github.com/hibiscus-software/hibiscus-lib'
        credentials {
            // Locally: gpr.user / gpr.key in ~/.gradle/gradle.properties (a token with read:packages).
            // In GitHub Actions: the workflow's GITHUB_ACTOR and GITHUB_TOKEN.
            username = providers.gradleProperty('gpr.user').orElse(providers.environmentVariable('GITHUB_ACTOR')).getOrNull()
            password = providers.gradleProperty('gpr.key').orElse(providers.environmentVariable('GITHUB_TOKEN')).getOrNull()
        }
        content { includeGroup 'software.hibiscus.hibiscuslib' }
    }
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    // The whole library: button bar, GUI toolkit, helpers and the Teams API. Loaded as a mod in dev runs.
    implementation "software.hibiscus.hibiscuslib:hibiscuslib:${hibiscuslib_version}"
}
```

A mod that only uses the Teams API (and makes Hibiscus Lib optional) can compile against the API jar alone:

```groovy
dependencies {
    compileOnly "software.hibiscus.hibiscuslib:hibiscuslib-api:1.0.0+26.3"
}
```

`~/.gradle/gradle.properties` (yours, never committed):

```properties
gpr.user=<your GitHub user name>
gpr.key=<a personal access token (classic) with read:packages>
```

In the consumer's workflows, pass the token to Gradle:

```yaml
      - name: Build with Gradle
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ github.token }}
        run: ./gradlew build
```

and give the consumer's repository read access to both packages once (package page → **Package settings** →
**Manage Actions access**).

Then declare the dependency in `META-INF/neoforge.mods.toml`:

```toml
[[dependencies.${mod_id}]]
    modId="hibiscuslib"
    type="required"
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

`ordering="AFTER"` makes your mod construct after Hibiscus Lib, so `TeamsApi.install(...)` (Hibiscus Teams) replaces
the fallback Hibiscus Lib has already set, and `HibiscusButtons.register(...)` calls land before key mappings are
collected.

## Working on Lib, Teams and Quests side by side

Two ways, both checked with a copy of the Hibiscus Teams template against this repository.

### `includeBuild` (recommended)

The consumer builds against your Hibiscus Lib checkout directly; a change in Lib is picked up by the next build of
Teams or Quests, with nothing to publish. Add to the consumer's `settings.gradle`:

```groovy
// Develop against a local Hibiscus Lib checkout instead of the published jar, when there is one.
// Point at it with -Phibiscuslib_dir=... or hibiscuslib_dir in ~/.gradle/gradle.properties; default ../hibiscus-lib.
def hibiscusLib = file(providers.gradleProperty('hibiscuslib_dir').getOrElse('../hibiscus-lib'))
if (hibiscusLib.isDirectory()) {
    includeBuild(hibiscusLib) {
        dependencySubstitution {
            substitute module('software.hibiscus.hibiscuslib:hibiscuslib') using project(':')
        }
    }
}
```

With the three repositories cloned next to each other (`hibiscus-lib/`, `hibiscus-teams/`, `hibiscus-quests/`) nothing
else is needed; otherwise set `hibiscuslib_dir`. The consumer's `runClient`, `runServer` and `runGameTestServer` then
load Hibiscus Lib from your checkout (built as a jar on the way), and Hibiscus Lib's own game tests run alongside
the consumer's.

When the directory isn't there (CI, or someone who only has Teams), the substitution is skipped and the GitHub
Packages version is used, so the same `settings.gradle` works everywhere.

### `mavenLocal`

Publish Lib to `~/.m2` and point the consumer at it:

```sh
# in hibiscus-lib/
./gradlew publishToMavenLocal
```

```groovy
// in the consumer's build.gradle, before the GitHub Packages repository
repositories {
    mavenLocal {
        content { includeGroup 'software.hibiscus.hibiscuslib' }
    }
}
```

Re-run `publishToMavenLocal` after each change to Lib. Remove `mavenLocal()` (or delete
`~/.m2/repository/software/hibiscus`) before release builds, so they can't pick up an unreleased local jar.

## What to read next

- [API.md](API.md): the Teams API, its events, and the toolkit's classes.
- `/hibiscuslib widgets` in a dev client shows every widget; `WidgetGalleryScreen` is a worked example of using them.
