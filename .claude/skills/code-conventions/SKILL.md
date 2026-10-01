---
name: code-conventions
description: Use whenever you name or create something - package, file, class, function, variable, test, Compose or SwiftUI view, resource key, route, endpoint, JSON field, database table or column - and for code formatting rules in Kotlin, Swift, SQL and Gradle.
---

# Naming and code conventions

Names are the documentation. A reader should know what a thing is and where it lives from its name alone.
All identifiers, file names and comments are English; Vietnamese appears only in string resources and content.

References:
- Kotlin coding conventions — https://kotlinlang.org/docs/coding-conventions.html
- Android Kotlin style guide — https://developer.android.com/kotlin/style-guide
- Compose API guidelines — https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md
- Swift API Design Guidelines — https://www.swift.org/documentation/api-design-guidelines/
- `_reference/nowinandroid/` — naming of repositories (`OfflineFirst…`), fakes (`Test…`/`Fake…`), UI state, previews.
- `_reference/kotlinconf-app/` — routes, backend routes/services/repositories, schema objects.
- `.editorconfig` — formatting rules enforced by the formatter.

## Packages, modules, files

- Root package `com.app.platform.language`; then module, then feature: `…language.shared.reading`,
  `…language.ui.reading`, `…language.backend.reading`. Lowercase, no underscores, singular feature names.
- Organise by **feature first**, layer second (`shared/…/reading/data/ReadingRepository.kt`, not
  `shared/…/data/ReadingRepository.kt`; layout per module in `kmp-feature`).
  Cross-feature code goes to `common/` only when two features use it.
- Gradle modules and directories: kebab-case (`exam-engine`, `ui-compose`).
- One top-level class per file, file named after it. A file of top-level functions is named after what they do
  (`ReadingMappers.kt`, `HttpClientFactory.kt`). Never `Utils.kt`, `Helpers.kt`, `Common.kt`, `Manager`, `Base…`.

## Kotlin names

| Kind | Rule | Example |
| --- | --- | --- |
| Class, interface, object, enum | PascalCase noun | `ReadingTest`, `BandScale` |
| Function | camelCase verb phrase | `submitAnswers()`, `toReadingTest()` |
| Property, variable, parameter | camelCase noun | `remainingSeconds`, `testId` |
| Boolean | `is`/`has`/`can`/`should` prefix | `isSubmitted`, `hasAudio` |
| `const val`, top-level immutable value | UPPER_SNAKE_CASE | `MAX_ATTEMPTS` |
| Enum entry | UPPER_SNAKE_CASE, `@SerialName` snake_case when serialised | `@SerialName("true_false_not_given") TRUE_FALSE_NOT_GIVEN` |
| Backing property | `_name` private, `name` public | `_state` / `state` |
| Acronyms | Capitalise only the first letter when longer than 2 | `HttpClient`, `IeltsBand`, `id`, `UiState` |
| Extension conversion | `to<Type>()` | `ResultRow.toReadingTest()` |
| Flow-returning | `observe<Thing>()` | `observeTests(): Flow<List<…>>` |
| One-shot suspend read | `get<Thing>()` | `getTest(id)` |
| Remote refresh | `refresh<Thing>()` | `refreshTests()` |

## Role suffixes

| Role | Name |
| --- | --- |
| Domain/API model | plain noun: `ReadingTest`, `Attempt` |
| Repository interface / implementation | `ReadingRepository` / `OfflineFirstReadingRepository`, `ExposedReadingRepository` |
| API client | `ReadingApi` |
| SQLDelight wrapper | `ReadingDao` |
| Error type | `ReadingError` (sealed) |
| ViewModel / UI state / one-off event | `ReadingSessionViewModel` / `ReadingSessionUiState` / `ReadingSessionEvent` |
| Backend service / routes function / table | `ReadingService` / `fun Route.readingRoutes()` in `ReadingRoutes.kt` / `ReadingTestsTable` |
| Koin module | `readingModule` (value), file `ReadingModule.kt` |
| Navigation route | `ReadingSessionRoute`, entries function `readingEntries()` |
| Test class / fake | `ReadingScorerTest` / `FakeReadingRepository` |
| Design-system component | `Lp` + name, same in Compose and SwiftUI: `LpButton` |

## Compose

- Composables that emit UI: PascalCase noun (`ReadingSessionScreen`, `LpAnswerChip`). Composables that return a value:
  camelCase (`rememberNavState()`).
- Screen entry: `<Feature>Screen`; sections: `<Feature><Part>` (`ReadingQuestionList`); previews:
  `private fun <Composable>Preview()` in `<Feature>PreviewData.kt` + the screen file.
- Parameter order: required data, `modifier: Modifier = Modifier` (first optional), other optional params, event
  lambdas named `on<Event>` (`onAnswerSelected`), trailing `content` lambda.

## Swift

- Types PascalCase, members camelCase, per Swift API Design Guidelines. Files named after the main type.
- Views `<Feature>View` (`ReadingSessionView`); components `Lp…` mirroring Compose; Swift-only helpers
  `<Thing>+<Purpose>.swift` for extensions (`ReadingTest+Display.swift`).
- Kotlin types keep their names in Swift; never rename them with typealiases.

## Resources and content

- String keys: snake_case, `<feature>_<screen>_<meaning>`: `reading_session_submit`, `common_retry`. Same keys in
  Compose resources and `Localizable.xcstrings`.
- Drawables/icons: `ic_<name>`; images: `img_<name>`; snake_case.
- Content files: `content/<skill>/<id>.json` with kebab-case ids (`cambridge-18-test-1`).
- Design tokens: dot-separated lowercase paths (`color.text.primary`, `spacing.md`).

## Backend, API, database

- HTTP paths: `/api/v1/<plural-resource>/{id}/<sub-resource>`, kebab-case, nouns only (`/api/v1/reading/tests/{id}/attempts`).
- JSON fields camelCase (kotlinx default); enum values snake_case via `@SerialName`. Timestamps ISO-8601 UTC
  (`Instant`), durations in whole seconds with a unit suffix (`durationSeconds`).
- Tables: snake_case plural (`reading_tests`); columns snake_case (`created_at`); primary key `id`; foreign key
  `<singular>_id`; indexes `idx_<table>_<columns>`; unique `uq_<table>_<columns>`; foreign key constraints
  `fk_<table>_<referenced>`. Every table has `created_at`, mutable ones `updated_at` (`timestamptz`).
- Migrations `V<NNN>__<snake_case_description>.sql`. SQL keywords uppercase.
- SQLDelight: tables snake_case, queries camelCase verbs (`selectById`, `upsertSummary`).
- Config keys: lowercase dotted (`database.url`); environment variables UPPER_SNAKE (`DATABASE_URL`).

## Formatting

- Formatter output wins: `./gradlew spotlessApply` (ktlint) for Kotlin/Gradle, `swift format` for Swift. Never fight it.
- **2-space indentation for every nesting level** (Kotlin, Gradle, Swift, SQL, JSON, YAML, XML, shell), continuation
  lines included; never 4 spaces or tabs. Python scripts follow PEP 8 (4). Max line length 120, trailing commas on multiline lists, no wildcard imports, no unused imports,
  newline at end of file.
- Expression bodies for one-expression functions; named arguments when a call has several of the same type or a boolean.
- Visibility: `private` / `internal` by default; `public` only for what other modules use. Explicit types on public API.
- Order inside a class: properties, `init`, public functions, private functions, companion object.

## Checklist

- [ ] Every new name follows the tables above; no `Utils`, `Manager`, `Base`, `Impl` names.
- [ ] Files live in the feature package and are named after their main declaration.
- [ ] Strings, routes, endpoints and tables follow their naming rule.
- [ ] Formatter run, no warnings.
