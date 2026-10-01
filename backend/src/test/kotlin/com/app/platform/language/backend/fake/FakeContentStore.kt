package com.app.platform.language.backend.fake

import com.app.platform.language.backend.reading.ContentStore
import com.app.platform.language.backend.reading.StoredReadingTest

class FakeContentStore(
  var tests: List<StoredReadingTest> = emptyList(),
) : ContentStore {
  var nextError: Throwable? = null

  override suspend fun readingTests(): List<StoredReadingTest> {
    nextError?.let { throw it }
    return tests
  }

  override suspend fun readingTest(id: String): StoredReadingTest? {
    nextError?.let { throw it }
    return tests.find { it.test.id == id }
  }
}
