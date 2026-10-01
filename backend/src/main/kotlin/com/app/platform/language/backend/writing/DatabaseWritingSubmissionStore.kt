package com.app.platform.language.backend.writing

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.WritingSubmission
import com.app.platform.language.core.model.WritingSubmissionStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

class DatabaseWritingSubmissionStore(
  private val database: AppDatabase,
) : WritingSubmissionStore {
  override suspend fun save(
    userId: Uuid,
    submission: NewWritingSubmission,
  ): WritingSubmission =
    database.tx {
      val id = Uuid.random()
      exec(
        INSERT_SQL,
        listOf(
          WritingSubmissionsTable.id.columnType to id,
          WritingSubmissionsTable.userId.columnType to userId,
          WritingSubmissionsTable.promptId.columnType to submission.promptId,
          WritingSubmissionsTable.text.columnType to submission.text,
          WritingSubmissionsTable.wordCount.columnType to submission.wordCount,
          WritingSubmissionsTable.status.columnType to WritingSubmissionStatus.PENDING.id,
        ),
      )
      checkNotNull(findById(userId, id))
    }

  override suspend fun list(userId: Uuid): List<WritingSubmission> =
    database.tx {
      WritingSubmissionsTable
        .selectAll()
        .where { WritingSubmissionsTable.userId eq userId }
        .orderBy(WritingSubmissionsTable.createdAt to SortOrder.DESC, WritingSubmissionsTable.id to SortOrder.DESC)
        .map { it.toWritingSubmission() }
    }

  override suspend fun find(
    userId: Uuid,
    id: Uuid,
  ): WritingSubmission? = database.tx { findById(userId, id) }

  private fun JdbcTransaction.findById(
    userId: Uuid,
    id: Uuid,
  ): WritingSubmission? =
    WritingSubmissionsTable
      .selectAll()
      .where { (WritingSubmissionsTable.userId eq userId) and (WritingSubmissionsTable.id eq id) }
      .singleOrNull()
      ?.toWritingSubmission()

  private fun ResultRow.toWritingSubmission() =
    WritingSubmission(
      id = this[WritingSubmissionsTable.id].toString(),
      promptId = this[WritingSubmissionsTable.promptId],
      text = this[WritingSubmissionsTable.text],
      wordCount = this[WritingSubmissionsTable.wordCount],
      submittedAt = this[WritingSubmissionsTable.createdAt].toInstant().toKotlinInstant(),
      status = WritingSubmissionStatus.entries.first { it.id == this[WritingSubmissionsTable.status] },
    )

  private val WritingSubmissionStatus.id: String get() = name.lowercase()

  private companion object {
    // clock_timestamp() keeps history order exact even for submissions stored within one transaction tick.
    val INSERT_SQL =
      """
      INSERT INTO writing_submissions (id, user_id, prompt_id, text, word_count, status, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, clock_timestamp(), clock_timestamp())
      """.trimIndent()
  }
}
