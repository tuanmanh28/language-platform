package com.app.platform.language.backend.reading

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.common.Versioned
import com.app.platform.language.backend.common.catalogVersion
import com.app.platform.language.backend.content.ContentAccessPolicy
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

class ReadingService(
  private val store: ContentStore,
  private val access: ContentAccessPolicy,
) {
  suspend fun listTests(viewer: AuthIdentity?): Result<Versioned<List<ReadingTestSummary>>, ReadingError> =
    runSuspendCatching { store.readingTests() }
      .mapError(ReadingError::Unexpected)
      .map { stored ->
        val tests = stored.filter { access.canSee(viewer, it.visibility) }
        Versioned(tests.map { it.test.toSummary() }, catalogVersion(tests.map { it.test.id to it.version }))
      }

  suspend fun getTest(
    id: String,
    viewer: AuthIdentity?,
  ): Result<Versioned<ReadingTest>, ReadingError> =
    findTest(id, viewer).map { stored -> Versioned(stored.test, stored.version.toString()) }

  suspend fun submit(
    id: String,
    request: SubmitAnswersRequest,
    viewer: AuthIdentity?,
  ): Result<ReadingResult, ReadingError> =
    findTest(id, viewer).map { stored ->
      ReadingScorer.score(stored.test, request.answers)
    }

  private suspend fun findTest(
    id: String,
    viewer: AuthIdentity?,
  ): Result<StoredReadingTest, ReadingError> =
    runSuspendCatching { store.readingTest(id) }
      .mapError(ReadingError::Unexpected)
      .andThen { stored ->
        stored?.takeIf { access.canSee(viewer, it.visibility) }.toResultOr { ReadingError.NotFound }
      }
}
