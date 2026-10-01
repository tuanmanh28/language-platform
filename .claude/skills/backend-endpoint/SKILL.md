---
name: backend-endpoint
description: Use when adding or changing a backend HTTP endpoint, a Ktor plugin, authentication on routes, request validation or the mapping of errors to HTTP responses.
---

# Backend endpoints (Ktor)

Layering: **route → service → repository**. Routes speak HTTP, services hold business rules and return `Result`,
repositories talk to the database.

References:
- `_reference/kotlinconf-app/backend/src/main/kotlin/org/jetbrains/kotlinconf/backend/` — `RoutesModule.kt`,
  `routes/*Routes.kt` (one file per area, dependencies via Koin `inject`), `services/`, `repositories/`, `di/*Module.kt`,
  `PluginsModule.kt` (StatusPages), `plugins/Authenticate.kt`.
- `_reference/kotlin-result/example/src/main/kotlin/com/github/michaelbull/result/example/` — services returning `Result`, routes folding them.
- `_reference/ktor-samples/jwt-auth-tests/` — authentication + tests.

## Layout

```
backend/src/main/kotlin/com/app/platform/language/backend/
├── Application.kt          main + Application.module() wiring plugins, DI and routes
├── config/                 AppConfig (from env)
├── plugins/                Serialization, CORS, CallId/logging, StatusPages, Authentication
├── di/                     Koin modules (config, repositories, services)
└── <feature>/
    ├── <Feature>Routes.kt   fun Route.<feature>Routes()
    ├── <Feature>Service.kt
    ├── <Feature>Repository.kt (+ Exposed tables, see backend-database)
    └── <Feature>Errors.kt   sealed error + toHttp()
```

## Adding an endpoint

1. Request/response models in `core/model` (`@Serializable`), so the apps reuse them.
2. Service function returns `Result<Response, <Feature>Error>`; validation errors are cases of the sealed error.
3. Route: parse input → call service → respond.

   ```kotlin
   fun Route.readingRoutes() {
     val service by inject<ReadingService>()

     route("/api/v1/reading/tests") {
       get("{id}") {
         service.getTest(call.parameters.getOrFail("id")).mapBoth(
           success = { call.respond(it) },
           failure = { error -> error.toHttp().let { (status, body) -> call.respond(status, body) } },
         )
       }
     }
   }
   ```

4. Auth: wrap routes in `authenticate("firebase") { }`; read the user with a small `call.requireUser()` helper.
5. Register service/repository in the Koin module; mount the route in `Application.module()`.
6. Describe the route for OpenAPI.
7. Tests with `testApplication` (see `testing`).

## Rules

- Routes contain no business logic and no database access.
- `StatusPages` only maps unexpected exceptions to 500 (and logs them). Expected failures never use exceptions.
- Never leak stack traces or internal messages; errors are `ApiError(message)` JSON.
- Configuration comes from `AppConfig` (environment variables), never `System.getenv` in features.
- Version the API path (`/api/v1`). Breaking changes need a new version.

## Checklist

- [ ] Route → service → repository; service returns `Result`.
- [ ] Input validated; auth applied where needed.
- [ ] Tests for 2xx, each 4xx, and unauthorized access.
- [ ] Models shared through `core/model`.
