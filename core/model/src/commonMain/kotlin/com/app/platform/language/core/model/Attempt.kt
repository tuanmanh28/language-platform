package com.app.platform.language.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SubmitAttemptRequest(
  val clientId: String,
  val testId: String,
  val answers: Map<String, String>,
  val createdAt: Instant,
)

@Serializable
data class Attempt(
  val id: String,
  val clientId: String,
  val testId: String,
  val correctCount: Int,
  val totalQuestions: Int,
  val band: Double,
  val answers: Map<String, String>,
  val createdAt: Instant,
  val syncedAt: Instant,
)

@Serializable
data class AttemptPage(
  val attempts: List<Attempt>,
  val nextCursor: String?,
)
