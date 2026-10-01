package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingTest

class BundledContentStore : ContentStore {
  override suspend fun readingTests(): List<StoredReadingTest> = BundledReadingTests.all.map { it.toStored() }

  override suspend fun readingTest(id: String): StoredReadingTest? = BundledReadingTests.find(id)?.toStored()

  private fun ReadingTest.toStored() = StoredReadingTest(this, BUNDLED_VERSION)

  private companion object {
    const val BUNDLED_VERSION = 1
  }
}
