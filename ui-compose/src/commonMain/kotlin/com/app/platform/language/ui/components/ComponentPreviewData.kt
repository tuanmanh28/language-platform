package com.app.platform.language.ui.components

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.QuestionType

internal object ComponentPreviewData {
  private val test = BundledReadingTests.all.first()

  val testSummary = test.toSummary()
  val choiceQuestion = test.allQuestions().first { it.options.isNotEmpty() }
  val explanation = test.allQuestions().firstNotNullOf { it.explanation?.text }
  val fixedChoice = QuestionType.TRUE_FALSE_NOT_GIVEN.fixedChoices.first()
  const val BAND = 6.5
  const val TEST_DETAILS = "Test details"
  const val EMPTY_TITLE = "Empty title"
  const val EMPTY_MESSAGE = "Empty message"
  const val ERROR_MESSAGE = "Error message"
  const val GAP_ANSWER = "solar power"
  const val GAP_ANSWER_TOO_LONG = "cheap solar power"
  const val TIMER_LABEL = "42:15"
  const val TIMER_WARNING_LABEL = "00:45"
}
