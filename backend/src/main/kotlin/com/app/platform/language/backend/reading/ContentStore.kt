package com.app.platform.language.backend.reading

import com.app.platform.language.core.model.ReadingTest

interface ContentStore {
  fun readingTests(): List<ReadingTest>

  fun readingTest(id: String): ReadingTest?
}
