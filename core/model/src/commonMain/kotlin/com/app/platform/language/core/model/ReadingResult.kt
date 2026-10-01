package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitAnswersRequest(
  val answers: Map<String, String>,
)

@Serializable
data class ReadingResult(
  val testId: String,
  val correctCount: Int,
  val totalQuestions: Int,
  val band: Double,
  val questionResults: List<QuestionResult>,
)

@Serializable
data class QuestionResult(
  val questionId: String,
  val number: Int,
  val userAnswer: String?,
  val isCorrect: Boolean,
  val acceptedAnswers: List<String>,
)
