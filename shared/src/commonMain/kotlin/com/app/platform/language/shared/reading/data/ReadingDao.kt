package com.app.platform.language.shared.reading.data

import co.touchlab.kermit.Logger
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.shared.db.LanguagePlatformQueries
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal class ReadingDao(
  private val queries: LanguagePlatformQueries,
  private val json: Json,
  private val dispatcher: CoroutineDispatcher,
) {
  private val log = Logger.withTag("ReadingDao")

  suspend fun findTest(id: String): Result<ReadingTest?, ReadingError> =
    access {
      queries.selectTest(id).executeAsOneOrNull()?.let(::decodeTestOrNull)
    }

  suspend fun findAllTests(): Result<List<ReadingTest>, ReadingError> =
    access {
      queries.selectAllTests().executeAsList().mapNotNull(::decodeTestOrNull)
    }

  suspend fun saveTest(
    test: ReadingTest,
    updatedAt: Instant,
  ): Result<Unit, ReadingError> =
    access {
      queries.upsertTest(test.id, json.encodeToString(ReadingTest.serializer(), test), updatedAt.toEpochMilliseconds())
    }

  suspend fun insertAttempt(
    result: ReadingResult,
    answers: Map<String, String>,
    createdAt: Instant,
  ): Result<Unit, ReadingError> =
    access {
      queries.insertAttempt(
        testId = result.testId,
        correctCount = result.correctCount.toLong(),
        totalQuestions = result.totalQuestions.toLong(),
        band = result.band,
        answersJson = json.encodeToString(answersSerializer, answers),
        createdAt = createdAt.toEpochMilliseconds(),
      )
    }

  private suspend fun <V> access(block: () -> V): Result<V, ReadingError> =
    runSuspendCatching { withContext(dispatcher) { block() } }
      .mapError { ReadingError.Unexpected(it) }

  private fun decodeTestOrNull(raw: String): ReadingTest? =
    try {
      json.decodeFromString(ReadingTest.serializer(), raw)
    } catch (e: IllegalArgumentException) {
      log.w(e) { "Corrupted cached test ignored" }
      null
    }

  private companion object {
    val answersSerializer = MapSerializer(String.serializer(), String.serializer())
  }
}
