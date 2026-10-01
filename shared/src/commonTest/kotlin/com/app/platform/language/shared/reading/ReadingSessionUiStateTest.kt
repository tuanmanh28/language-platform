package com.app.platform.language.shared.reading

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.QuestionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingSessionUiStateTest {
  private val test = BundledReadingTests.all.first().copy(timeLimitMinutes = 10)
  private val questionId =
    test.passages
      .flatMap { it.questionGroups }
      .first { it.type == QuestionType.SENTENCE_COMPLETION }
      .questions
      .first()
      .id

  private fun inProgress(
    remainingSeconds: Int = 600,
    answers: Map<String, String> = emptyMap(),
  ) = ReadingSessionUiState.InProgress(test, answers, remainingSeconds)

  @Test
  fun remainingFractionIsTheShareOfTheTimeLimitLeft() {
    assertEquals(1f, inProgress(remainingSeconds = 600).remainingFraction)
    assertEquals(0.25f, inProgress(remainingSeconds = 150).remainingFraction)
    assertEquals(0f, inProgress(remainingSeconds = 0).remainingFraction)
  }

  @Test
  fun remainingFractionIsZeroWithoutATimeLimit() {
    val untimed = ReadingSessionUiState.InProgress(test.copy(timeLimitMinutes = 0), emptyMap(), remainingSeconds = 0)

    assertEquals(0f, untimed.remainingFraction)
  }

  @Test
  fun timeIsRunningOutFromTheLastMinute() {
    assertFalse(inProgress(remainingSeconds = 61).isTimeRunningOut)
    assertTrue(inProgress(remainingSeconds = 60).isTimeRunningOut)
    assertTrue(inProgress(remainingSeconds = 0).isTimeRunningOut)
  }

  @Test
  fun wordCountIgnoresExtraSpacesAndEdgePunctuation() {
    val state = inProgress(answers = mapOf(questionId to "  solar   power. "))

    assertEquals(2, state.wordCountFor(questionId))
  }

  @Test
  fun wordCountOfAnUnansweredQuestionIsZero() {
    assertEquals(0, inProgress().wordCountFor(questionId))
  }

  @Test
  fun wordCountOfAnUnknownQuestionIsZero() {
    val state = inProgress(answers = mapOf("unknown" to "solar power"))

    assertEquals(0, state.wordCountFor("unknown"))
  }
}
