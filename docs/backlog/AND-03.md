# AND-03 — App navigation shell

- **Lane:** android
- **Depends on:** F-01
- **Verify:** `./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Replace the hand-rolled screen switching with real navigation and the app's main tabs.

## Scope
- Use JetBrains `navigation-compose` (multiplatform) with type-safe routes in `ui-compose`.
- Bottom navigation (Android) / navigation rail (wide screens & desktop): **Luyện tập** (practice home), **Từ vựng**, **Tiến độ**, **Tôi**. Non-implemented tabs show an `EmptyState` placeholder.
- Practice home lists skills (Reading enabled; Listening/Writing/Speaking marked "Sắp có").
- Reading list → session → result flows through routes; each back-stack entry owns its ViewModels (remove `ScopedViewModels` workaround in `App.kt`).
- Android back handling and process-death-safe route restoration.

## Acceptance criteria
- Navigation tests (JVM or instrumented) cover tab switching and list → session → back.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on the task branch as `AND-03: <summary>`.
