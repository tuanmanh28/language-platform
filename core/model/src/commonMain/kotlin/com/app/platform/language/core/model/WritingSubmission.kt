package com.app.platform.language.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SubmitWritingRequest(
  val promptId: String,
  val text: String,
)

@Serializable
data class WritingSubmission(
  val id: String,
  val promptId: String,
  val text: String,
  val wordCount: Int,
  val submittedAt: Instant,
  val status: WritingSubmissionStatus,
)

@Serializable
enum class WritingSubmissionStatus {
  @SerialName("pending")
  PENDING,

  @SerialName("grading")
  GRADING,

  @SerialName("graded")
  GRADED,

  @SerialName("failed")
  FAILED,
}
