---
name: backend-database
description: Use when changing the backend PostgreSQL schema, writing Flyway migrations, defining Exposed tables or queries, seeding content, or testing against a real database.
---

# Backend database (PostgreSQL + Flyway + Exposed)

Flyway owns the schema. Exposed (DSL, `org.jetbrains.exposed.v1.*`) runs queries. Tests use a real Postgres via
Testcontainers.

References:
- `_reference/kotlinconf-app/backend/src/main/kotlin/org/jetbrains/kotlinconf/backend/schema/` — table objects;
  `repositories/KotlinConfRepository.kt` — `suspendTransaction` queries with Exposed v1 imports.
- `_reference/kotlinconf-app/backend/src/main/resources/db/migrations/` — versioned SQL naming (`V001__initial_schema.sql`).
  Do **not** copy their hand-written `MigrationRunner`; we use Flyway.
- Exposed, Flyway and Testcontainers official docs for the latest versions.

## Migrations

- `backend/src/main/resources/db/migration/V<NNN>__<snake_case_description>.sql`, three-digit sequence.
- Forward-only. Never edit or delete a migration that may have run anywhere; add a new one.
- Every new column is nullable or has a default unless the table is empty in all environments.
- Index columns used in `WHERE`, `JOIN` and `ORDER BY`. Foreign keys explicit, with `ON DELETE` chosen deliberately.
- Flyway runs on startup: `Flyway.configure().dataSource(dataSource).load().migrate()`.

## Exposed

```kotlin
internal object ReadingTestsTable : Table("reading_tests") {
  val id = text("id")
  val title = text("title")
  val content = text("content")
  val published = bool("published")
  override val primaryKey = PrimaryKey(id)
}

internal class ExposedReadingRepository(private val database: Database) : ReadingRepository {
  override suspend fun find(id: String): ReadingTest? = suspendTransaction(database) {
    ReadingTestsTable.selectAll()
      .where { (ReadingTestsTable.id eq id) and ReadingTestsTable.published }
      .singleOrNull()
      ?.toReadingTest()
  }
}
```

- Table objects mirror migrations exactly; Exposed never creates or alters schema (`SchemaUtils` only in tests if at all).
- One repository per aggregate; it returns domain models, never `ResultRow`.
- Keep transactions short; no network calls inside a transaction.
- JSON content stored as `jsonb` is (de)serialized with `ContentJson` from `core/model`.
- Repositories may throw only on bugs; the service wraps calls with `runSuspendCatching` and maps to the feature error.

## Seeding

Seed tasks are idempotent upserts keyed by natural ids and only bump a version when content changed.

## Tests

- Testcontainers `postgres` (latest stable image tag) started once per test class; run Flyway, then the repository.
- Skip with a clear message when Docker is unavailable (`Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable)`).
- Each test starts from a clean state (truncate tables or a fresh schema).

## Checklist

- [ ] New migration file, none edited.
- [ ] Table object matches the migration; indexes for queried columns.
- [ ] Repository maps rows to domain models.
- [ ] Testcontainers test covers the new queries.
