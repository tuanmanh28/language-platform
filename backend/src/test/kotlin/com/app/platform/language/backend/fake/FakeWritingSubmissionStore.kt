package com.app.platform.language.backend.fake

import com.app.platform.language.backend.writing.NewWritingSubmission
import com.app.platform.language.backend.writing.WritingSubmissionStore
import com.app.platform.language.core.model.WritingSubmission
import com.app.platform.language.core.model.WritingSubmissionStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

class FakeWritingSubmissionStore : WritingSubmissionStore {
  private val submissionsByUser = mutableMapOf<Uuid, MutableList<WritingSubmission>>()
  private var clock = Instant.parse("2026-10-01T10:00:00Z")
  var nextError: Throwable? = null

  fun submissionsOf(userId: Uuid): List<WritingSubmission> = submissionsByUser[userId].orEmpty()

  override suspend fun save(
    userId: Uuid,
    submission: NewWritingSubmission,
  ): WritingSubmission {
    nextError?.let { throw it }
    clock += 1.minutes
    val stored =
      WritingSubmission(
        id = Uuid.random().toString(),
        promptId = submission.promptId,
        text = submission.text,
        wordCount = submission.wordCount,
        submittedAt = clock,
        status = WritingSubmissionStatus.PENDING,
      )
    submissionsByUser.getOrPut(userId) { mutableListOf() }.add(stored)
    return stored
  }

  override suspend fun list(userId: Uuid): List<WritingSubmission> {
    nextError?.let { throw it }
    return submissionsOf(userId).sortedByDescending { it.submittedAt }
  }

  override suspend fun find(
    userId: Uuid,
    id: Uuid,
  ): WritingSubmission? {
    nextError?.let { throw it }
    return submissionsOf(userId).find { it.id == id.toString() }
  }
}
