package com.app.platform.language.backend.attempt

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

internal object ReadingAttemptsTable : Table("reading_attempts") {
  val id = uuid("id")
  val userId = uuid("user_id")
  val clientId = text("client_id")
  val testId = text("test_id")
  val correctCount = integer("correct_count")
  val totalQuestions = integer("total_questions")
  val band = decimal("band", precision = 2, scale = 1)

  // Read-only mapping of the JSONB column; writes cast explicitly in DatabaseAttemptStore.
  val answers = text("answers")
  val createdAt = timestampWithTimeZone("created_at")
  val syncedAt = timestampWithTimeZone("synced_at")
  override val primaryKey = PrimaryKey(id)
}
