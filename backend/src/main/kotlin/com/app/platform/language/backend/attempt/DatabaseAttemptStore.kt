package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.core.model.Attempt
import com.app.platform.language.core.model.ContentJson
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

class DatabaseAttemptStore(
  private val database: AppDatabase,
) : AttemptStore {
  override suspend fun find(
    userId: Uuid,
    clientId: String,
  ): Attempt? = database.tx { findByClientId(userId, clientId) }

  override suspend fun save(
    userId: Uuid,
    attempt: NewAttempt,
  ): SubmittedAttempt =
    database.tx {
      val isNew =
        exec(
          INSERT_SQL,
          listOf(
            ReadingAttemptsTable.userId.columnType to userId,
            ReadingAttemptsTable.clientId.columnType to attempt.clientId,
            ReadingAttemptsTable.testId.columnType to attempt.testId,
            ReadingAttemptsTable.correctCount.columnType to attempt.correctCount,
            ReadingAttemptsTable.totalQuestions.columnType to attempt.totalQuestions,
            ReadingAttemptsTable.band.columnType to BigDecimal.valueOf(attempt.band),
            ReadingAttemptsTable.answers.columnType to ContentJson.encodeToString(answersSerializer, attempt.answers),
            ReadingAttemptsTable.createdAt.columnType to attempt.createdAt.toOffsetDateTime(),
          ),
          explicitStatementType = StatementType.SELECT,
        ) { inserted -> inserted.next() } == true
      SubmittedAttempt(checkNotNull(findByClientId(userId, attempt.clientId)), isNew)
    }

  override suspend fun list(
    userId: Uuid,
    since: Instant?,
    after: AttemptCursor?,
    limit: Int,
  ): List<Attempt> =
    database.tx {
      ReadingAttemptsTable
        .selectAll()
        .where {
          (ReadingAttemptsTable.userId eq userId)
            .andIfNotNull(since?.let { ReadingAttemptsTable.syncedAt greater it.toOffsetDateTime() })
            .andIfNotNull(after?.let(::olderThan))
        }.orderBy(ReadingAttemptsTable.syncedAt to SortOrder.DESC, ReadingAttemptsTable.id to SortOrder.DESC)
        .limit(limit)
        .map { it.toAttempt() }
    }

  private fun olderThan(cursor: AttemptCursor): Op<Boolean> {
    val syncedAt = cursor.syncedAt.toOffsetDateTime()
    return (ReadingAttemptsTable.syncedAt less syncedAt) or
      ((ReadingAttemptsTable.syncedAt eq syncedAt) and (ReadingAttemptsTable.id less cursor.id))
  }

  private fun JdbcTransaction.findByClientId(
    userId: Uuid,
    clientId: String,
  ): Attempt? =
    ReadingAttemptsTable
      .selectAll()
      .where { (ReadingAttemptsTable.userId eq userId) and (ReadingAttemptsTable.clientId eq clientId) }
      .singleOrNull()
      ?.toAttempt()

  private fun ResultRow.toAttempt() =
    Attempt(
      id = this[ReadingAttemptsTable.id].toString(),
      clientId = this[ReadingAttemptsTable.clientId],
      testId = this[ReadingAttemptsTable.testId],
      correctCount = this[ReadingAttemptsTable.correctCount],
      totalQuestions = this[ReadingAttemptsTable.totalQuestions],
      band = this[ReadingAttemptsTable.band].toDouble(),
      answers = ContentJson.decodeFromString(answersSerializer, this[ReadingAttemptsTable.answers]),
      createdAt = this[ReadingAttemptsTable.createdAt].toInstant().toKotlinInstant(),
      syncedAt = this[ReadingAttemptsTable.syncedAt].toInstant().toKotlinInstant(),
    )

  private fun Instant.toOffsetDateTime(): OffsetDateTime = toJavaInstant().atOffset(ZoneOffset.UTC)

  private companion object {
    val answersSerializer = MapSerializer(String.serializer(), String.serializer())

    // A retried upload keeps the first attempt; clock_timestamp() keeps synced_at close to commit time for since queries.
    val INSERT_SQL =
      """
      INSERT INTO reading_attempts
        (user_id, client_id, test_id, correct_count, total_questions, band, answers, created_at, synced_at)
      VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, clock_timestamp())
      ON CONFLICT (user_id, client_id) DO NOTHING
      RETURNING id
      """.trimIndent()
  }
}
