package com.app.platform.language.core.model

import com.app.platform.language.core.model.content.BundledContent
import kotlinx.serialization.json.Json

/** Cấu hình JSON dùng chung cho app, backend và nội dung đề. */
val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

/** Các đề mẫu được nhúng sẵn lúc build (xem :core:model:generateBundledContent). */
object BundledReadingTests {
    val all: List<ReadingTest> by lazy {
        BundledContent.readingTestsJson.map { ContentJson.decodeFromString(ReadingTest.serializer(), it) }
    }

    fun find(id: String): ReadingTest? = all.firstOrNull { it.id == id }
}
