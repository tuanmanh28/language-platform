package com.app.platform.language.backend.reading

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.common.loadContentFiles
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
  suspend fun seed(directory: Path): Result<Int, SeedError> =
    loadContentFiles(directory, ReadingTest.serializer()).andThen { tests ->
      runSuspendCatching { database.tx { tests.forEach { upsert(it) } } }
        .map { tests.size }
        .mapError(SeedError::WriteFailed)
    }

  private fun JdbcTransaction.upsert(test: ReadingTest) {
    exec(
      UPSERT_SQL,
      listOf(
        ReadingTestsTable.id.columnType to test.id,
        ReadingTestsTable.module.columnType to test.module.serialName(),
        ReadingTestsTable.title.columnType to test.title,
        ReadingTestsTable.timeLimitMinutes.columnType to test.timeLimitMinutes,
        ReadingTestsTable.content.columnType to ContentJson.encodeToString(ReadingTest.serializer(), test),
      ),
    )
  }

  private fun IeltsModule.serialName(): String =
    ContentJson.encodeToJsonElement(IeltsModule.serializer(), this).jsonPrimitive.content

  private companion object {
    // Only rows whose content changed are updated, so reseeding the same files never bumps the version.
    val UPSERT_SQL =
      """
      INSERT INTO reading_tests (id, module, title, time_limit_minutes, content, published)
      VALUES (?, ?, ?, ?, CAST(? AS JSONB), TRUE)
      ON CONFLICT (id) DO UPDATE SET
        module = EXCLUDED.module,
        title = EXCLUDED.title,
        time_limit_minutes = EXCLUDED.time_limit_minutes,
        content = EXCLUDED.content,
        version = reading_tests.version + 1,
        updated_at = now()
      WHERE reading_tests.content <> EXCLUDED.content
      """.trimIndent()
  }
}
