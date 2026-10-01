---
name: shared-data-layer
description: Use when the apps call the backend, cache data locally with SQLDelight, add or migrate app database tables, or sync data created offline - anything in the repository/API/database layer of the shared KMP module.
---

# Shared data layer (Ktor client + SQLDelight, offline-first)

The local database is the source of truth. The network fills it. The UI only ever reads the database.

References:
- `_reference/nowinandroid/core/data/src/main/kotlin/com/google/samples/apps/nowinandroid/core/data/repository/OfflineFirstNewsRepository.kt` and `.../SyncUtilities.kt` — offline-first reads, sync contract.
- `_reference/KaMPKit/shared/src/commonMain/kotlin/co/touchlab/kampkit/DatabaseHelper.kt`, `.../models/BreedRepository.kt` — SQLDelight flows, staleness.
- `_reference/kotlinconf-app/app/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/di/AppBindings.kt` — Ktor client plugins.
- Current code: `shared/.../network/ReadingApi.kt`, `shared/.../data/ReadingRepository.kt`, `shared/src/commonMain/sqldelight/`.

## Building blocks

1. **API class** per backend area. Thin: one function per endpoint, returns `Result<Dto, <Feature>Error>`.

   ```kotlin
   class ReadingApi(private val client: HttpClient) {
       suspend fun getTest(id: String): Result<ReadingTest, ReadingError> =
           runSuspendCatching { client.get("reading/tests/$id").body<ReadingTest>() }
               .mapError { it.toReadingError() }
   }
   ```

   The `HttpClient` is configured once in DI with `DefaultRequest` (base URL), `ContentNegotiation`, `HttpTimeout`,
   `HttpRequestRetry` (idempotent requests only) and auth. API classes never build URLs from a base URL themselves.

2. **SQLDelight** schema in `shared/src/commonMain/sqldelight/com/app/platform/language/shared/db/`.
   - One `.sq` file per table group, named queries in verb form (`selectById`, `upsert`, `deleteOlderThan`).
   - Any schema change after the first release adds `<n>.sqm` with the migration; never edit an old one.
   - Observe with `asFlow().mapToList(dispatcher)` / `mapToOneOrNull(dispatcher)`.

3. **Repository**: interface in the feature package, implementation named after its strategy.

   ```kotlin
   interface ReadingRepository {
       fun observeTests(): Flow<List<ReadingTestSummary>>
       suspend fun refreshTests(): Result<Unit, ReadingError>
       suspend fun getTest(id: String): Result<ReadingTest, ReadingError>
   }

   internal class OfflineFirstReadingRepository(
       private val api: ReadingApi,
       private val dao: ReadingDao,
   ) : ReadingRepository {

       override fun observeTests() = dao.observeSummaries()

       override suspend fun refreshTests() = api.listTests().map { dao.replaceSummaries(it) }

       override suspend fun getTest(id: String) =
           dao.findTest(id)?.let { Ok(it) }
               ?: api.getTest(id).onOk { dao.saveTest(it) }
   }
   ```

   - Wrap generated SQLDelight queries in a small `Dao` class so repositories stay readable and testable.
   - Never call the network from `init`; the ViewModel triggers refresh.
   - Data created offline (attempts, saved words) is written locally first with a `pendingSync` flag, then uploaded by a
     `sync()` function that returns `Result` and is safe to call repeatedly.

4. **Platform pieces** (`expect`/`actual` or Koin `platformModule`): HTTP engine and SQL driver only.

## Tests

- API: Ktor `MockEngine` returning canned JSON; assert both `Ok` and each `Err` mapping.
- DAO/repository: in-memory driver (`JdbcSqliteDriver(IN_MEMORY)` in `jvmTest`) and a fake API.
- Offline: fake API returning `Err(Offline)` → repository still serves cached data.

## Checklist

- [ ] Reads are `Flow` from the database; writes/sync are `suspend` returning `Result`.
- [ ] No URL building, JSON parsing or SQL in ViewModels.
- [ ] Schema changes come with a `.sqm` migration and a migration test.
- [ ] Works offline: covered by a test.
