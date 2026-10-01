package com.app.platform.language.shared.reading.data

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.github.michaelbull.result.Result

internal interface ReadingRepository {
  suspend fun getTests(): ReadingTestCatalog

  suspend fun getTest(id: String): Result<ReadingTest, ReadingError>

  suspend fun saveAttempt(
    result: ReadingResult,
    answers: Map<String, String>,
  ): Result<Unit, ReadingError>
}
