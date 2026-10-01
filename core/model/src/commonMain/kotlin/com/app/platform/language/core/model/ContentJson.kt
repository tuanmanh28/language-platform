package com.app.platform.language.core.model

import com.app.platform.language.core.model.content.BundledContent
import kotlinx.serialization.json.Json

/** JSON configuration shared by the apps, the backend and test content. */
val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

/** Sample tests embedded at build time (see :core:model:generateBundledContent). */
object BundledReadingTests {
    val all: List<ReadingTest> by lazy {
        BundledContent.readingTestsJson.map { ContentJson.decodeFromString(ReadingTest.serializer(), it) }
    }

    fun find(id: String): ReadingTest? = all.firstOrNull { it.id == id }
}
