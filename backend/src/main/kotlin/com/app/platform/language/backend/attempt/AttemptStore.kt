package com.app.platform.language.backend.attempt

import com.app.platform.language.core.model.Attempt
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface AttemptStore {
  suspend fun find(
    userId: Uuid,
    clientId: String,
  ): Attempt?

  suspend fun save(
    userId: Uuid,
    attempt: NewAttempt,
  ): SubmittedAttempt

  suspend fun list(
    userId: Uuid,
    since: Instant?,
    after: AttemptCursor?,
    limit: Int,
  ): List<Attempt>
}

data class NewAttempt(
  val clientId: String,
  val testId: String,
  val answers: Map<String, String>,
  val correctCount: Int,
  val totalQuestions: Int,
  val band: Double,
  val createdAt: Instant,
)
