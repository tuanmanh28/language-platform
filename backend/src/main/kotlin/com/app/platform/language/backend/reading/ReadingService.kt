package com.app.platform.language.backend.reading

import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.core.model.SubmitAnswersRequest
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.toResultOr

class ReadingService(
  private val store: ContentStore,
) {
  fun listTests(): List<ReadingTestSummary> = store.readingTests().map { it.toSummary() }

  fun getTest(id: String): Result<ReadingTest, ReadingError> =
    store.readingTest(id).toResultOr { ReadingError.NotFound }

  fun submit(
    id: String,
    request: SubmitAnswersRequest,
  ): Result<ReadingResult, ReadingError> = getTest(id).map { test -> ReadingScorer.score(test, request.answers) }
}
