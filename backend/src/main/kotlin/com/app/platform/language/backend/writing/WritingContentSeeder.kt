package com.app.platform.language.backend.writing

import com.app.platform.language.backend.common.SeedError
import com.app.platform.language.backend.common.loadContentFiles
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.media.isPrivateMediaPath
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.WritingPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.nio.file.Path

class WritingContentSeeder(
  private val database: AppDatabase,
) {
  fun load(directory: Path): Result<List<WritingPrompt>, SeedError> =
    loadContentFiles(directory, WritingPrompt.serializer())

  suspend fun seed(
    directory: Path,
    visibility: Visibility,
  ): Result<Int, SeedError> = load(directory).andThen { seed(it, visibility) }

  suspend fun seed(
    prompts: List<WritingPrompt>,
    visibility: Visibility,
  ): Result<Int, SeedError> =
    (if (visibility == Visibility.PRIVATE) validatePrivate(prompts) else Ok(Unit)).andThen {
      runSuspendCatching { database.tx { prompts.forEach { upsert(it, visibility) } } }
        .map { prompts.size }
        .mapError(SeedError::WriteFailed)
    }

  // Private images are served from <folder>/<file name> in CONTENT_DIR/images or R2, so other paths never load.
  fun validatePrivate(prompts: List<WritingPrompt>): Result<Unit, SeedError> {
    val invalid =
      prompts.firstNotNullOfOrNull { prompt ->
        prompt.imageUrl
          ?.takeUnless(::isPrivateMediaPath)
          ?.let { SeedError.InvalidPrivateImagePath(prompt.id, it) }
      }
    return if (invalid == null) Ok(Unit) else Err(invalid)
  }

  private fun JdbcTransaction.upsert(
    prompt: WritingPrompt,
    visibility: Visibility,
  ) {
    exec(
      UPSERT_SQL,
      listOf(
        WritingPromptsTable.id.columnType to prompt.id,
        WritingPromptsTable.content.columnType to ContentJson.encodeToString(WritingPrompt.serializer(), prompt),
        WritingPromptsTable.visibility.columnType to visibility.id,
      ),
    )
  }

  private companion object {
    // Only changed rows are updated, so reseeding the same files never bumps the version.
    val UPSERT_SQL =
      """
      INSERT INTO writing_prompts (id, content, visibility, published)
      VALUES (?, CAST(? AS JSONB), ?, TRUE)
      ON CONFLICT (id) DO UPDATE SET
        content = EXCLUDED.content,
        visibility = EXCLUDED.visibility,
        version = writing_prompts.version + 1,
        updated_at = now()
      WHERE (writing_prompts.content, writing_prompts.visibility)
        IS DISTINCT FROM (EXCLUDED.content, EXCLUDED.visibility)
      """.trimIndent()
  }
}
