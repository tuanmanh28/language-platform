package com.app.platform.language.backend.fake

import com.app.platform.language.backend.listening.ListeningContentStore
import com.app.platform.language.backend.listening.StoredListeningTest

class FakeListeningContentStore(
  var tests: List<StoredListeningTest> = emptyList(),
) : ListeningContentStore {
  var nextError: Throwable? = null

  override suspend fun listeningTests(): List<StoredListeningTest> {
    nextError?.let { throw it }
    return tests
  }

  override suspend fun listeningTest(id: String): StoredListeningTest? {
    nextError?.let { throw it }
    return tests.find { it.test.id == id }
  }
}
