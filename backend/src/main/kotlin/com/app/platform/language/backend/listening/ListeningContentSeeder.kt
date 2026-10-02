package com.app.platform.language.backend.listening

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.common.loadContentFiles
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.media.isPrivateMediaPath
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.ListeningTest
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
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
  fun load(directory: Path): Result<List<ListeningTest>, SeedError> =
    loadContentFiles(directory, ListeningTest.serializer())

  suspend fun seed(
    directory: Path,
    visibility: Visibility,
  ): Result<Int, SeedError> = load(directory).andThen { seed(it, visibility) }

  suspend fun seed(
    tests: List<ListeningTest>,
    visibility: Visibility,
  ): Result<Int, SeedError> =
    (if (visibility == Visibility.PRIVATE) validatePrivate(tests) else Ok(Unit)).andThen {
      runSuspendCatching { database.tx { tests.forEach { upsert(it, visibility) } } }
        .map { tests.size }
        .mapError(SeedError::WriteFailed)
    }

  // Private audio is served from <test-id>/<file name> in CONTENT_DIR/audio or the R2 bucket, so other paths never play.
  fun validatePrivate(tests: List<ListeningTest>): Result<Unit, SeedError> {
    val invalid =
      tests.firstNotNullOfOrNull { test ->
        test.sections
          .map { it.audioUrl }
          .find { !isPrivateMediaPath(it) }
          ?.let { SeedError.InvalidPrivateAudioPath(test.id, it) }
      }
    return if (invalid == null) Ok(Unit) else Err(invalid)
  }

  private fun JdbcTransaction.upsert(
    test: ListeningTest,
    visibility: Visibility,
  ) {
    exec(
      UPSERT_SQL,
      listOf(
        ListeningTestsTable.id.columnType to test.id,
        ListeningTestsTable.title.columnType to test.title,
        ListeningTestsTable.content.columnType to ContentJson.encodeToString(ListeningTest.serializer(), test),
        ListeningTestsTable.visibility.columnType to visibility.id,
      ),
    )
  }

  private companion object {
    // Only changed rows are updated, so reseeding the same files never bumps the version.
    val UPSERT_SQL =
      """
      INSERT INTO listening_tests (id, title, content, visibility, published)
      VALUES (?, ?, CAST(? AS JSONB), ?, TRUE)
      ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        content = EXCLUDED.content,
        visibility = EXCLUDED.visibility,
        version = listening_tests.version + 1,
        updated_at = now()
      WHERE (listening_tests.content, listening_tests.visibility)
        IS DISTINCT FROM (EXCLUDED.content, EXCLUDED.visibility)
      """.trimIndent()
  }
}
