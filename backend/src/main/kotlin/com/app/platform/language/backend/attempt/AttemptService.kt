package com.app.platform.language.backend.attempt

import com.app.platform.language.backend.reading.ContentStore
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.AttemptPage
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.SubmitAttemptRequest
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class AttemptService(
  private val attempts: AttemptStore,
  private val content: ContentStore,
) {
  suspend fun submit(
    userId: Uuid,
    request: SubmitAttemptRequest,
  ): Result<SubmittedAttempt, AttemptError> =
    validate(request)
      .andThen { access { attempts.find(userId, request.clientId) } }
      .andThen { existing ->
        if (existing != null) {
          Ok(SubmittedAttempt(existing, isNew = false))
        } else {
          findTest(request.testId)
            .andThen { test -> validateAnswers(request.answers, test) }
            .andThen { test -> access { attempts.save(userId, request.scoredAgainst(test)) } }
        }
      }

  suspend fun listAttempts(
    userId: Uuid,
    query: AttemptQuery,
  ): Result<AttemptPage, AttemptError> =
    validate(query)
      .andThen { access { attempts.list(userId, query.since?.minus(SINCE_OVERLAP), query.cursor, query.limit + 1) } }
      .map { found ->
        val page = found.take(query.limit)
        AttemptPage(
          page,
          nextCursor =
            if (found.size >
              query.limit
            ) {
              AttemptCursor.after(page.last()).encode()
            } else {
              null
            },
        )
      }

  private fun validate(request: SubmitAttemptRequest): Result<Unit, AttemptError> =
    when {
      request.clientId.isBlank() -> {
        Err(AttemptError.InvalidRequest("clientId must not be blank"))
      }

      request.clientId.length > MAX_CLIENT_ID_LENGTH -> {
        Err(AttemptError.InvalidRequest("clientId must be at most $MAX_CLIENT_ID_LENGTH characters"))
      }

      else -> {
        Ok(Unit)
      }
    }

  private fun validateAnswers(
    answers: Map<String, String>,
    test: ReadingTest,
  ): Result<ReadingTest, AttemptError> {
    val questionIds = test.allQuestions().map { it.id }.toSet()
    return when {
      !questionIds.containsAll(answers.keys) -> {
        Err(AttemptError.InvalidRequest("answers must only use question ids of the test"))
      }

      answers.values.any { it.length > MAX_ANSWER_LENGTH } -> {
        Err(AttemptError.InvalidRequest("answers must be at most $MAX_ANSWER_LENGTH characters"))
      }

      else -> {
        Ok(test)
      }
    }
  }

  private fun validate(query: AttemptQuery): Result<Unit, AttemptError> =
    if (query.limit in 1..AttemptQuery.MAX_LIMIT) {
      Ok(Unit)
    } else {
      Err(AttemptError.InvalidRequest("limit must be between 1 and ${AttemptQuery.MAX_LIMIT}"))
    }

  private suspend fun findTest(id: String): Result<ReadingTest, AttemptError> =
    access { content.readingTest(id) }.andThen { stored -> stored?.test.toResultOr { AttemptError.TestNotFound } }

  private fun SubmitAttemptRequest.scoredAgainst(test: ReadingTest): NewAttempt {
    val result = ReadingScorer.score(test, answers)
    return NewAttempt(
      clientId = clientId,
      testId = test.id,
      answers = answers,
      correctCount = result.correctCount,
      totalQuestions = result.totalQuestions,
      band = result.band,
      createdAt = createdAt,
    )
  }

  private suspend fun <V> access(block: suspend () -> V): Result<V, AttemptError> =
    runSuspendCatching { block() }.mapError(AttemptError::Unexpected)

  private companion object {
    const val MAX_CLIENT_ID_LENGTH = 64
    const val MAX_ANSWER_LENGTH = 200

    // An attempt can commit after a later-stamped one was read, so since re-reads a short window.
    val SINCE_OVERLAP = 30.seconds
  }
}
