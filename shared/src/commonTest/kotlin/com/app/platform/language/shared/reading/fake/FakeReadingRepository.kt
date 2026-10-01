package com.app.platform.language.shared.reading.fake

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.shared.reading.data.ReadingRepository
import com.app.platform.language.shared.reading.data.ReadingTestCatalog
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

internal class FakeReadingRepository : ReadingRepository {
  var tests: List<ReadingTest> = emptyList()
  var isOffline: Boolean = false
  var nextTestError: ReadingError? = null
  var nextSaveError: ReadingError? = null
  val savedAttempts = mutableListOf<ReadingResult>()

  override suspend fun getTests(): ReadingTestCatalog = ReadingTestCatalog(tests.map { it.toSummary() }, isOffline)

  override suspend fun getTest(id: String): Result<ReadingTest, ReadingError> =
    nextTestError?.let { Err(it) } ?: tests.firstOrNull { it.id == id }.toResultOr { ReadingError.NotFound }

  override suspend fun saveAttempt(
    result: ReadingResult,
    answers: Map<String, String>,
  ): Result<Unit, ReadingError> {
    nextSaveError?.let { return Err(it) }
    savedAttempts += result
    return Ok(Unit)
  }
}
