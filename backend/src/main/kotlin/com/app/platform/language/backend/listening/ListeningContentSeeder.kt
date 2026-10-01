package com.app.platform.language.backend.listening

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.common.loadContentFiles
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.nio.file.Path

class ListeningContentSeeder(
  private val database: AppDatabase,
) {
  suspend fun seed(directory: Path): Result<Int, SeedError> =
    loadContentFiles(directory, ListeningTest.serializer()).andThen { tests ->
      runSuspendCatching { database.tx { tests.forEach { upsert(it) } } }
        .map { tests.size }
        .mapError(SeedError::WriteFailed)
    }

  private fun JdbcTransaction.upsert(test: ListeningTest) {
    exec(
      UPSERT_SQL,
      listOf(
        ListeningTestsTable.id.columnType to test.id,
        ListeningTestsTable.title.columnType to test.title,
        ListeningTestsTable.content.columnType to ContentJson.encodeToString(ListeningTest.serializer(), test),
      ),
    )
  }

  private companion object {
    // Only rows whose content changed are updated, so reseeding the same files never bumps the version.
    val UPSERT_SQL =
      """
      INSERT INTO listening_tests (id, title, content, published)
      VALUES (?, ?, CAST(? AS JSONB), TRUE)
      ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        content = EXCLUDED.content,
        version = listening_tests.version + 1,
        updated_at = now()
      WHERE listening_tests.content <> EXCLUDED.content
      """.trimIndent()
  }
}
