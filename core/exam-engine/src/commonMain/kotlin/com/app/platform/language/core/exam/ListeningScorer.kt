package com.app.platform.language.core.exam

import com.app.platform.language.core.model.ListeningResult
import com.app.platform.language.core.model.ListeningTest

object ListeningScorer {
  fun score(
    test: ListeningTest,
    answers: Map<String, String>,
  ): ListeningResult {
    val results = scoreQuestionGroups(test.sections.flatMap { it.questionGroups }, answers)
    val correct = results.count { it.isCorrect }
    return ListeningResult(
      testId = test.id,
      correctCount = correct,
      totalQuestions = results.size,
      band = if (results.isEmpty()) 0.0 else BandScale.listeningBand(correct, results.size),
      questionResults = results,
    )
  }
}
