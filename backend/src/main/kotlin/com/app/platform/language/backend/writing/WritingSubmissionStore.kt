package com.app.platform.language.backend.writing

import com.app.platform.language.core.model.WritingSubmission
import kotlin.uuid.Uuid

interface WritingSubmissionStore {
  suspend fun save(
    userId: Uuid,
    submission: NewWritingSubmission,
  ): WritingSubmission

  suspend fun list(userId: Uuid): List<WritingSubmission>

  suspend fun find(
    userId: Uuid,
    id: Uuid,
  ): WritingSubmission?
}

data class NewWritingSubmission(
  val promptId: String,
  val text: String,
  val wordCount: Int,
)
