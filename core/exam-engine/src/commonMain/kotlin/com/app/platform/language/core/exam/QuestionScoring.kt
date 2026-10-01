package com.app.platform.language.core.exam

import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionResult

internal fun scoreQuestionGroups(
  groups: List<QuestionGroup>,
  answers: Map<String, String>,
): List<QuestionResult> =
  groups.flatMap { group ->
    group.questions.map { question -> evaluate(group, question, answers[question.id]) }
  }

private fun evaluate(
  group: QuestionGroup,
  question: Question,
  raw: String?,
): QuestionResult {
  val userAnswer = raw?.takeIf { it.isNotBlank() }
  val normalizedUser = userAnswer?.let { AnswerNormalizer.normalize(it, group.type) }
  val accepted = question.acceptedAnswers.map { AnswerNormalizer.normalize(it, group.type) }

  // Copy to a local: properties of classes from another module cannot be smart-cast.
  val maxWords = group.maxWords
  val withinWordLimit =
    maxWords == null ||
      normalizedUser == null ||
      AnswerNormalizer.wordCount(normalizedUser) <= maxWords

  return QuestionResult(
    questionId = question.id,
    number = question.number,
    userAnswer = userAnswer,
    isCorrect = normalizedUser != null && withinWordLimit && normalizedUser in accepted,
    acceptedAnswers = question.acceptedAnswers,
    explanation = question.explanation,
  )
}
