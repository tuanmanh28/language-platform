package com.app.platform.language.core.exam

import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionResult
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest

/**
 * Chấm một bài Reading. Cùng một code chạy trong app (chấm offline) và trên backend,
 * nên kết quả luôn khớp nhau.
 */
object ReadingScorer {

    /** @param answers questionId -> câu trả lời thô người học nhập. */
    fun score(test: ReadingTest, answers: Map<String, String>): ReadingResult {
        val results = test.passages.flatMap { passage ->
            passage.questionGroups.flatMap { group ->
                group.questions.map { question -> evaluate(group, question, answers[question.id]) }
            }
        }
        val correct = results.count { it.isCorrect }
        return ReadingResult(
            testId = test.id,
            correctCount = correct,
            totalQuestions = results.size,
            band = if (results.isEmpty()) 0.0 else BandScale.readingBand(test.module, correct, results.size),
            questionResults = results,
        )
    }

    private fun evaluate(group: QuestionGroup, question: Question, raw: String?): QuestionResult {
        val userAnswer = raw?.takeIf { it.isNotBlank() }
        val normalizedUser = userAnswer?.let { AnswerNormalizer.normalize(it, group.type) }
        val accepted = question.acceptedAnswers.map { AnswerNormalizer.normalize(it, group.type) }

        // Gán ra biến local: property của class ở module khác không smart-cast được.
        val maxWords = group.maxWords
        val withinWordLimit = maxWords == null ||
            normalizedUser == null ||
            AnswerNormalizer.wordCount(normalizedUser) <= maxWords

        return QuestionResult(
            questionId = question.id,
            number = question.number,
            userAnswer = userAnswer,
            isCorrect = normalizedUser != null && withinWordLimit && normalizedUser in accepted,
            acceptedAnswers = question.acceptedAnswers,
        )
    }
}
