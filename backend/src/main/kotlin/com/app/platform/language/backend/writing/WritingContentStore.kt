package com.app.platform.language.backend.writing

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.core.model.WritingPrompt

interface WritingContentStore {
  suspend fun writingPrompts(): List<StoredWritingPrompt>

  suspend fun writingPrompt(id: String): StoredWritingPrompt?
}

data class StoredWritingPrompt(
  val prompt: WritingPrompt,
  val version: Int,
  val visibility: Visibility,
)
