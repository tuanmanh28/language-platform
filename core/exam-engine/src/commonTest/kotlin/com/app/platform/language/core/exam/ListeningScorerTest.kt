package com.app.platform.language.core.exam

import com.app.platform.language.core.model.BundledListeningTests
import com.app.platform.language.core.model.ChoiceOption
import com.app.platform.language.core.model.ListeningSection
import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListeningScorerTest {
  private val test =
    ListeningTest(
      id = "l1",
      title = "Unit test",
      sections =
        listOf(
          section(
            number = 1,
            QuestionGroup(
              id = "g1",
              type = QuestionType.SENTENCE_COMPLETION,
              instruction = "NO MORE THAN TWO WORDS AND/OR A NUMBER",
              maxWords = 2,
              questions = listOf(Question("q1", 1, "Postcode", acceptedAnswers = listOf("BR4 7TN"))),
            ),
          ),
          section(
            number = 2,
            QuestionGroup(
              id = "g2",
              type = QuestionType.MULTIPLE_CHOICE,
              instruction = "Choose A or B",
              questions =
                listOf(
                  Question(
                    "q2",
                    2,
                    "Pick one",
                    options = listOf(ChoiceOption("A", "a"), ChoiceOption("B", "b")),
                    acceptedAnswers = listOf("A"),
                  ),
                ),
            ),
          ),
        ),
    )

  private val sample = BundledListeningTests.all.first()

  @Test
  fun scoresQuestionsFromEverySection() {
    val result = ListeningScorer.score(test, mapOf("q1" to " br4  7tn.", "q2" to "a"))

    assertEquals(2, result.correctCount)
    assertEquals(2, result.totalQuestions)
    assertEquals(listOf("q1", "q2"), result.questionResults.map { it.questionId })
  }

  @Test
  fun missingAndTooLongAnswersAreWrong() {
    val result = ListeningScorer.score(test, mapOf("q1" to "postcode BR4 7TN"))

    assertEquals(0, result.correctCount)
    assertFalse(result.questionResults.first { it.questionId == "q1" }.isCorrect)
    assertEquals(null, result.questionResults.first { it.questionId == "q2" }.userAnswer)
  }

  @Test
  fun bundledSampleCanBeFullyAnsweredFromItsKey() {
    val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }

    val result = ListeningScorer.score(sample, perfect)

    assertEquals(sample.questionCount, result.correctCount)
    assertEquals(9.0, result.band)
  }

  @Test
  fun resultCarriesTheQuestionExplanation() {
    val result = ListeningScorer.score(sample, emptyMap())

    assertEquals(sample.allQuestions().map { it.explanation }, result.questionResults.map { it.explanation })
    assertTrue(result.questionResults.all { it.explanation != null })
  }

  @Test
  fun bandUsesTheListeningScale() {
    val answers = sample.allQuestions().take(32).associate { it.id to it.acceptedAnswers.first() }

    val result = ListeningScorer.score(sample, answers)

    assertEquals(40, result.totalQuestions)
    assertEquals(BandScale.listeningBand(32, 40), result.band)
    assertEquals(7.5, result.band)
  }

  @Test
  fun testWithoutQuestionsScoresZero() {
    val result = ListeningScorer.score(test.copy(sections = emptyList()), emptyMap())

    assertEquals(0, result.totalQuestions)
    assertEquals(0.0, result.band)
  }

  private fun section(
    number: Int,
    group: QuestionGroup,
  ) = ListeningSection(
    id = "s$number",
    number = number,
    title = "Section $number",
    audioUrl = "listening/l1/section-$number.mp3",
    durationSeconds = 300,
    questionGroups = listOf(group),
  )
}
