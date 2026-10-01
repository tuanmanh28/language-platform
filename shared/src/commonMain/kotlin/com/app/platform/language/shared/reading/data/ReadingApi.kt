package com.app.platform.language.shared.reading.data

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.ReadingTestSummary
import com.github.michaelbull.result.Result

internal interface ReadingApi {
  suspend fun listTests(): Result<List<ReadingTestSummary>, ReadingError>

  suspend fun getTest(id: String): Result<ReadingTest, ReadingError>
}
