package com.app.platform.language.backend.writing

import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.core.model.BundledWritingPrompts
import com.app.platform.language.core.model.WritingPrompt

class BundledWritingContentStore : WritingContentStore {
  override suspend fun writingPrompts(): List<StoredWritingPrompt> = BundledWritingPrompts.all.map { it.toStored() }

  override suspend fun writingPrompt(id: String): StoredWritingPrompt? = BundledWritingPrompts.find(id)?.toStored()

  private fun WritingPrompt.toStored() = StoredWritingPrompt(this, BUNDLED_VERSION, Visibility.PUBLIC)

  private companion object {
    const val BUNDLED_VERSION = 1
  }
}
