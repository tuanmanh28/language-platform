package com.app.platform.language.backend.fake

import com.app.platform.language.backend.attempt.AttemptCursor
import com.app.platform.language.backend.attempt.AttemptStore
import com.app.platform.language.backend.attempt.NewAttempt
import com.app.platform.language.backend.attempt.SubmittedAttempt
import com.app.platform.language.core.model.Attempt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeAttemptStore : AttemptStore {
  private val attemptsByUser = mutableMapOf<Uuid, MutableList<Attempt>>()
  private var clock = Instant.parse("2026-10-01T10:00:00Z")
  var syncStep: Duration = 1.minutes
  var nextError: Throwable? = null

  fun attemptsOf(userId: Uuid): List<Attempt> = attemptsByUser[userId].orEmpty()

  override suspend fun find(
    userId: Uuid,
    clientId: String,
  ): Attempt? {
    nextError?.let { throw it }
    return attemptsOf(userId).find { it.clientId == clientId }
  }

  override suspend fun save(
    userId: Uuid,
    attempt: NewAttempt,
  ): SubmittedAttempt {
    nextError?.let { throw it }
    find(userId, attempt.clientId)?.let { return SubmittedAttempt(it, isNew = false) }
    clock += syncStep
    val stored =
      Attempt(
        id = Uuid.random().toString(),
        clientId = attempt.clientId,
        testId = attempt.testId,
        correctCount = attempt.correctCount,
        totalQuestions = attempt.totalQuestions,
        band = attempt.band,
        answers = attempt.answers,
        createdAt = attempt.createdAt,
        syncedAt = clock,
      )
    attemptsByUser.getOrPut(userId) { mutableListOf() }.add(stored)
    return SubmittedAttempt(stored, isNew = true)
  }

  override suspend fun list(
    userId: Uuid,
    since: Instant?,
    after: AttemptCursor?,
    limit: Int,
  ): List<Attempt> {
    nextError?.let { throw it }
    return attemptsOf(userId)
      .filter { since == null || it.syncedAt > since }
      .filter { after == null || it.isOlderThan(after) }
      .sortedWith(compareByDescending<Attempt> { it.syncedAt }.thenByDescending { it.id })
      .take(limit)
  }

  // Canonical lowercase UUID strings sort like PostgreSQL compares uuid values.
  private fun Attempt.isOlderThan(cursor: AttemptCursor): Boolean =
    syncedAt < cursor.syncedAt || (syncedAt == cursor.syncedAt && id < cursor.id.toString())
}
