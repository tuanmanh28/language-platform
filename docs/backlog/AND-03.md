# AND-03 — App navigation shell

- **Type / branch:** `feat` / `feat/navigation-shell`
- **Lane:** android
- **Depends on:** R-01, S-03
- **Verify:** `./gradlew spotlessCheck :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Replace the hand-rolled screen switching with real navigation and the app's main tabs.

## Scope
- Use JetBrains multiplatform **Navigation 3** (`org.jetbrains.androidx.navigation3:navigation3-ui`, latest stable) following the `navigation` skill and `_reference/kotlinconf-app/.../navigation/` (Routes, NavState, Navigator, NavHost).
- Bottom navigation (Android) / navigation rail (wide screens & desktop): **Luyện tập** (practice home), **Từ vựng**, **Tiến độ**, **Tôi**. Non-implemented tabs show an `EmptyState` placeholder.
- Practice home lists skills (Reading enabled; Listening/Writing/Speaking marked "Sắp có").
- Reading list → session → result flows through routes; each back-stack entry owns its ViewModels (remove `ScopedViewModels` workaround in `App.kt`).
- Android back handling and process-death-safe route restoration.

## Acceptance criteria
- Navigation tests (JVM or instrumented) cover tab switching and list → session → back.

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/navigation-shell` as `feat: <summary>` (no task id in the message).
