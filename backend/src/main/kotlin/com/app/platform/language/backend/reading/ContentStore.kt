package com.app.platform.language.backend.reading

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.core.model.ReadingTest

interface ContentStore {
  suspend fun readingTests(): List<StoredReadingTest>

  suspend fun readingTest(id: String): StoredReadingTest?
}

data class StoredReadingTest(
  val test: ReadingTest,
  val version: Int,
  val visibility: Visibility,
)
