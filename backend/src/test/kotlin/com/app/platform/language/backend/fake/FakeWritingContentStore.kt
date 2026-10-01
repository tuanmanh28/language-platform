package com.app.platform.language.backend.fake

import com.app.platform.language.backend.writing.StoredWritingPrompt
import com.app.platform.language.backend.writing.WritingContentStore

class FakeWritingContentStore(
  private val prompts: List<StoredWritingPrompt> = emptyList(),
) : WritingContentStore {
  var nextError: Throwable? = null

  override suspend fun writingPrompts(): List<StoredWritingPrompt> {
    nextError?.let { throw it }
    return prompts
  }

  override suspend fun writingPrompt(id: String): StoredWritingPrompt? {
    nextError?.let { throw it }
    return prompts.find { it.prompt.id == id }
  }
}
