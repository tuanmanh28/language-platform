package com.app.platform.language.core.exam

import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest

object ReadingScorer {
  fun score(
    test: ReadingTest,
    answers: Map<String, String>,
  ): ReadingResult {
    val results = scoreQuestionGroups(test.passages.flatMap { it.questionGroups }, answers)
    val correct = results.count { it.isCorrect }
    return ReadingResult(
      testId = test.id,
      correctCount = correct,
      totalQuestions = results.size,
      band = if (results.isEmpty()) 0.0 else BandScale.readingBand(test.module, correct, results.size),
      questionResults = results,
    )
  }
}
