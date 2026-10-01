package com.app.platform.language.backend.reading

import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import java.security.MessageDigest

class ReadingService(
  private val store: ContentStore,
) {
  suspend fun listTests(): Result<Versioned<List<ReadingTestSummary>>, ReadingError> =
    runSuspendCatching { store.readingTests() }
      .mapError(ReadingError::Unexpected)
      .map { tests -> Versioned(tests.map { it.test.toSummary() }, catalogVersion(tests)) }

  suspend fun getTest(id: String): Result<Versioned<ReadingTest>, ReadingError> =
    findTest(id).map { stored -> Versioned(stored.test, stored.version.toString()) }

  suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
  ): Result<ReadingResult, ReadingError> =
    findTest(id).map { stored ->
      ReadingScorer.score(stored.test, request.answers)
    }

  private suspend fun findTest(id: String): Result<StoredReadingTest, ReadingError> =
    runSuspendCatching { store.readingTest(id) }
      .mapError(ReadingError::Unexpected)
      .andThen { stored -> stored.toResultOr { ReadingError.NotFound } }

  private fun catalogVersion(tests: List<StoredReadingTest>): String {
    val fingerprint = tests.joinToString(",") { "${it.test.id}:${it.version}" }
    return MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray()).toHexString()
  }
}
