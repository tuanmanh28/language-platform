# AND-07 — Sign-in with Google and authenticated API client

- **Type / branch:** `feat` / `feat/and-07-google-sign-in`
- **Lane:** android
- **Depends on:** AND-03, BE-04
- **Verify:** `./gradlew :shared:jvmTest :app-android:assembleDebug :app-desktop:compileKotlin`

## Goal
Users sign in so attempts sync to the backend (BE-05 consumes this).

## Scope
- Firebase Auth on Android with Google sign-in (Credential Manager). Firebase config file is **not** committed: read it from `app-android/google-services.json` (gitignored) and document setup in README.
- `shared`: `AuthTokenProvider` interface (platform implementations), Ktor client attaches `Authorization: Bearer` and refreshes on 401.
- Tôi tab: sign in / sign out, show account; the app stays fully usable signed out.

## Acceptance criteria
- `shared` tests for header attachment and 401 refresh with a fake provider.
- Builds without `google-services.json` present (sign-in disabled with a clear message).

## Definition of done
- Verify command passes.
- Follows `CLAUDE.md` (scope, architecture, tests, no AI attribution in commits).
- Committed on branch `feat/and-07-google-sign-in` as `feat: <summary>` with `Task: AND-07` in the commit body.
