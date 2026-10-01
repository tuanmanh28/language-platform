package com.app.platform.language.backend.listening

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ListeningTest

class BundledListeningContentStore : ListeningContentStore {
  override suspend fun listeningTests(): List<StoredListeningTest> = BundledListeningTests.all.map { it.toStored() }

  override suspend fun listeningTest(id: String): StoredListeningTest? = BundledListeningTests.find(id)?.toStored()

  private fun ListeningTest.toStored() = StoredListeningTest(this, BUNDLED_VERSION, Visibility.PUBLIC)

  private companion object {
    const val BUNDLED_VERSION = 1
  }
}
