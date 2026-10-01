package com.app.platform.language.backend.reading

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.common.loadContentFiles
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.IeltsModule
import com.app.platform.language.core.model.ReadingTest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.nio.file.Path

class ReadingContentSeeder(
  private val database: AppDatabase,
) {
  fun load(directory: Path): Result<List<ReadingTest>, SeedError> =
    loadContentFiles(directory, ReadingTest.serializer())

  suspend fun seed(
    directory: Path,
    visibility: Visibility,
  ): Result<Int, SeedError> = load(directory).andThen { seed(it, visibility) }

  suspend fun seed(
    tests: List<ReadingTest>,
    visibility: Visibility,
  ): Result<Int, SeedError> =
    runSuspendCatching { database.tx { tests.forEach { upsert(it, visibility) } } }
      .map { tests.size }
      .mapError(SeedError::WriteFailed)

  private fun JdbcTransaction.upsert(
    test: ReadingTest,
    visibility: Visibility,
  ) {
    exec(
      UPSERT_SQL,
      listOf(
        ReadingTestsTable.id.columnType to test.id,
        ReadingTestsTable.module.columnType to test.module.serialName(),
        ReadingTestsTable.title.columnType to test.title,
        ReadingTestsTable.timeLimitMinutes.columnType to test.timeLimitMinutes,
        ReadingTestsTable.content.columnType to ContentJson.encodeToString(ReadingTest.serializer(), test),
        ReadingTestsTable.visibility.columnType to visibility.id,
      ),
    )
  }

  private fun IeltsModule.serialName(): String =
    ContentJson.encodeToJsonElement(IeltsModule.serializer(), this).jsonPrimitive.content

  private companion object {
    // Only changed rows are updated, so reseeding the same files never bumps the version.
    val UPSERT_SQL =
      """
      INSERT INTO reading_tests (id, module, title, time_limit_minutes, content, visibility, published)
      VALUES (?, ?, ?, ?, CAST(? AS JSONB), ?, TRUE)
      ON CONFLICT (id) DO UPDATE SET
        module = EXCLUDED.module,
        title = EXCLUDED.title,
        time_limit_minutes = EXCLUDED.time_limit_minutes,
        content = EXCLUDED.content,
        visibility = EXCLUDED.visibility,
        version = reading_tests.version + 1,
        updated_at = now()
      WHERE (reading_tests.content, reading_tests.visibility)
        IS DISTINCT FROM (EXCLUDED.content, EXCLUDED.visibility)
      """.trimIndent()
  }
}
