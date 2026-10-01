package com.app.platform.language.backend.writing

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.WritingPrompt
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class DatabaseWritingContentStore(
  private val database: AppDatabase,
) : WritingContentStore {
  override suspend fun writingPrompts(): List<StoredWritingPrompt> =
    database.tx {
      WritingPromptsTable
        .selectAll()
        .where { WritingPromptsTable.published eq true }
        .orderBy(WritingPromptsTable.id)
        .map { it.toStoredWritingPrompt() }
    }

  override suspend fun writingPrompt(id: String): StoredWritingPrompt? =
    database.tx {
      WritingPromptsTable
        .selectAll()
        .where { (WritingPromptsTable.id eq id) and (WritingPromptsTable.published eq true) }
        .singleOrNull()
        ?.toStoredWritingPrompt()
    }

  private fun ResultRow.toStoredWritingPrompt() =
    StoredWritingPrompt(
      prompt = ContentJson.decodeFromString(WritingPrompt.serializer(), this[WritingPromptsTable.content]),
      version = this[WritingPromptsTable.version],
      visibility = Visibility.fromId(this[WritingPromptsTable.visibility]),
    )
}
