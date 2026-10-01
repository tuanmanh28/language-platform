package com.app.platform.language.core.model

import com.app.platform.language.core.model.content.BundledContent
import kotlinx.serialization.json.Json

val ContentJson: Json =
  Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
  }

object BundledReadingTests {
  val all: List<ReadingTest> by lazy {
    BundledContent.readingTestsJson.map { ContentJson.decodeFromString(ReadingTest.serializer(), it) }
  }

  fun find(id: String): ReadingTest? = all.firstOrNull { it.id == id }
}

object BundledListeningTests {
  val all: List<ListeningTest> by lazy {
    BundledContent.listeningTestsJson.map { ContentJson.decodeFromString(ListeningTest.serializer(), it) }
  }

  fun find(id: String): ListeningTest? = all.firstOrNull { it.id == id }
}
