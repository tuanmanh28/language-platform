package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

/** Body của POST /api/v1/reading/tests/{id}/submit — map questionId -> câu trả lời thô của người học. */
@Serializable
data class SubmitAnswersRequest(
    val answers: Map<String, String>,
)

@Serializable
data class ReadingResult(
    val testId: String,
    val correctCount: Int,
    val totalQuestions: Int,
    /** Band ước tính (0.0–9.0, bước 0.5). */
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
