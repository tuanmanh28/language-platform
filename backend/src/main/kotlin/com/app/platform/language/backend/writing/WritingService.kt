package com.app.platform.language.backend.writing

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.content.ContentAccessPolicy
import com.app.platform.language.backend.content.Visibility
import com.app.platform.language.backend.media.MediaStorage
import com.app.platform.language.core.exam.WritingWordCounter
import com.app.platform.language.core.model.SubmitWritingRequest
import com.app.platform.language.core.model.WritingPrompt
import com.app.platform.language.core.model.WritingSubmission
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import kotlin.uuid.Uuid

class WritingService(
  private val prompts: WritingContentStore,
  private val submissions: WritingSubmissionStore,
  private val publicImages: MediaStorage,
  private val privateImages: MediaStorage,
  private val contentAccess: ContentAccessPolicy,
) {
  suspend fun listPrompts(viewer: AuthIdentity?): Result<List<WritingPrompt>, WritingError> =
    access { prompts.writingPrompts() }.map { stored ->
      stored.filter { contentAccess.canSee(viewer, it.visibility) }.map { it.withImageUrl() }
    }

  suspend fun submit(
    userId: Uuid,
    viewer: AuthIdentity?,
    request: SubmitWritingRequest,
  ): Result<WritingSubmission, WritingError> =
    validate(request)
      .andThen { findPrompt(request.promptId, viewer) }
      .andThen { prompt ->
        val submission = NewWritingSubmission(prompt.id, request.text, WritingWordCounter.count(request.text))
        access { submissions.save(userId, submission) }
      }

  suspend fun listSubmissions(userId: Uuid): Result<List<WritingSubmission>, WritingError> =
    access { submissions.list(userId) }

  suspend fun getSubmission(
    userId: Uuid,
    id: String,
  ): Result<WritingSubmission, WritingError> {
    val submissionId = Uuid.parseOrNull(id) ?: return Err(WritingError.SubmissionNotFound)
    return access { submissions.find(userId, submissionId) }
      .andThen { it.toResultOr { WritingError.SubmissionNotFound } }
  }

  private fun validate(request: SubmitWritingRequest): Result<Unit, WritingError> =
    when {
      request.text.isBlank() -> {
        Err(WritingError.InvalidRequest("text must not be blank"))
      }

      request.text.length > MAX_TEXT_LENGTH -> {
        Err(WritingError.InvalidRequest("text must be at most $MAX_TEXT_LENGTH characters"))
      }

      // PostgreSQL text columns cannot store NUL, which JSON can still carry as \u0000.
      '\u0000' in request.text -> {
        Err(WritingError.InvalidRequest("text must not contain NUL characters"))
      }

      else -> {
        Ok(Unit)
      }
    }

  private suspend fun findPrompt(
    id: String,
    viewer: AuthIdentity?,
  ): Result<WritingPrompt, WritingError> =
    access { prompts.writingPrompt(id) }.andThen { stored ->
      stored
        ?.takeIf { contentAccess.canSee(viewer, it.visibility) }
        ?.prompt
        .toResultOr { WritingError.PromptNotFound }
    }

  private fun StoredWritingPrompt.withImageUrl(): WritingPrompt {
    val images =
      when (visibility) {
        Visibility.PUBLIC -> publicImages
        Visibility.PRIVATE -> privateImages
      }
    return prompt.copy(imageUrl = prompt.imageUrl?.let(images::urlFor))
  }

  private suspend fun <V> access(block: suspend () -> V): Result<V, WritingError> =
    runSuspendCatching { block() }.mapError(WritingError::Unexpected)

  private companion object {
    // Far above any real IELTS answer, yet small enough that one request cannot bloat the table.
    const val MAX_TEXT_LENGTH = 20_000
  }
}
