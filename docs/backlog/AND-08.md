# AND-08 — Listening player and practice

- **Lane:** android
- **Depends on:** AND-02, AND-03, BE-08
- **Verify:** `./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Listening practice on Android.

## Scope
- `shared`: Listening repository/ViewModel mirroring Reading (offline cache of test JSON; audio downloaded to app storage for offline use).
- Android player with Media3 ExoPlayer behind an expect/actual or interface (desktop can be a stub for now): play/pause, ±5 s, speed 0.75–1.25×, section switching; exam mode disables seeking.
- Questions UI reuses AND-02 components; after submit show transcript with the answer location highlighted.

## Acceptance criteria
- ViewModel tests for exam mode vs practice mode and offline fallback.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on the task branch as `AND-08: <summary>`.
