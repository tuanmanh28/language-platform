package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

/** Body of POST /api/v1/reading/tests/{id}/submit — questionId -> the learner's raw answer. */
@Serializable
data class SubmitAnswersRequest(
    val answers: Map<String, String>,
)

@Serializable
data class ReadingResult(
    val testId: String,
    val correctCount: Int,
    val totalQuestions: Int,
    /** Estimated band (0.0–9.0, in 0.5 steps). */
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
