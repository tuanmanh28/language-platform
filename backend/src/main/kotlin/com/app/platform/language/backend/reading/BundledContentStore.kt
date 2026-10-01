package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingTest

class BundledContentStore : ContentStore {
  override fun readingTests(): List<ReadingTest> = BundledReadingTests.all

  override fun readingTest(id: String): ReadingTest? = BundledReadingTests.find(id)
}
