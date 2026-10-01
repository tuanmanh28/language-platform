package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ListeningResult(
  val testId: String,
  val correctCount: Int,
  val totalQuestions: Int,
  val band: Double,
  val questionResults: List<QuestionResult>,
)
