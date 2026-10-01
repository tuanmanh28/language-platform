package com.app.platform.language.shared.reading.fake

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.reading.data.ReadingApi
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

internal class FakeReadingApi : ReadingApi {
  var tests: List<ReadingTest> = emptyList()
  var nextError: ReadingError? = null

  override suspend fun listTests(): Result<List<ReadingTestSummary>, ReadingError> =
    nextError?.let { Err(it) } ?: Ok(tests.map { it.toSummary() })

  override suspend fun getTest(id: String): Result<ReadingTest, ReadingError> =
    nextError?.let { Err(it) } ?: tests.firstOrNull { it.id == id }.toResultOr { ReadingError.NotFound }
}
