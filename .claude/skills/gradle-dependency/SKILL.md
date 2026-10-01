---
name: gradle-dependency
description: Use when adding, upgrading or removing a library, Gradle plugin or Kotlin target, or when editing any build.gradle.kts, settings.gradle.kts or gradle/libs.versions.toml.
---

# Dependencies and Gradle

**Always use the latest stable version.** Use a pre-release only when no stable release exists for what the task needs,
and say so in the summary.

## Adding or upgrading a library

1. Find the latest stable version from the official source: Maven Central (`central.sonatype.com`), Google Maven
   (`maven.google.com`), the Gradle Plugin Portal, or the project's GitHub releases. Never guess a version.
2. Check compatibility with the project's Kotlin and AGP versions (release notes). If the newest version needs a newer
   Kotlin/AGP/Compose, upgrade those too — staying on the latest stack is the default.
3. For `commonMain`, confirm the artifact publishes every target we build: `android`, `jvm`, `iosArm64`,
   `iosSimulatorArm64`, `macosArm64`.
4. Add it to `gradle/libs.versions.toml`:
   - a `[versions]` entry when several artifacts share a version, otherwise `version.ref` to its own key;
   - library key in kebab-case grouped by family (`ktor-client-core`, `ktor-server-auth`).
5. Use it through the catalog (`libs.ktor.client.core`) in the narrowest source set (`commonMain` only if all targets need
   it; otherwise `androidMain`/`appleMain`/`jvmMain`). Prefer `implementation`; `api` only when the type is part of the
   module's public API.
6. Build: `./gradlew build --configuration-cache` for the touched modules, plus
   `:shared:compileKotlinIosSimulatorArm64` when `shared` or `core` changed.

## Upgrading the whole stack

When asked to update everything: Kotlin, AGP, Gradle wrapper (`./gradlew wrapper --gradle-version <latest>`), Compose
Multiplatform, then libraries. Read each release's migration notes, fix deprecations instead of suppressing them, and run
the full verify command from `docs/backlog`.

## Rules

- No versions or repository URLs hard-coded in build files.
- No new repositories without a reason in the summary.
- Shared build logic goes in convention plugins (`build-logic/`) once two modules repeat the same configuration.
- Keep the configuration cache green.
- Remove dependencies that are no longer used.

## Checklist

- [ ] Version is the latest stable and was looked up, not remembered.
- [ ] Every KMP target still compiles.
- [ ] Commit message explains notable upgrades (`update: ktor 3.6.0`).
