package com.app.platform.language.core.exam

import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ChoiceOption
import com.app.platform.language.core.model.Paragraph
import com.app.platform.language.core.model.Passage
import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionType
import com.app.platform.language.core.model.ReadingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingScorerTest {
    private val test =
        ReadingTest(
            id = "t1",
            title = "Unit test",
            timeLimitMinutes = 10,
            passages =
                listOf(
                    Passage(
                        id = "p1",
                        title = "Passage",
                        paragraphs = listOf(Paragraph(text = "Text")),
                        questionGroups =
                            listOf(
                                QuestionGroup(
                                    id = "g1",
                                    type = QuestionType.TRUE_FALSE_NOT_GIVEN,
                                    instruction = "TFNG",
                                    questions =
                                        listOf(
                                            Question("q1", 1, "Statement", acceptedAnswers = listOf("TRUE")),
                                        ),
                                ),
                                QuestionGroup(
                                    id = "g2",
                                    type = QuestionType.MULTIPLE_CHOICE,
                                    instruction = "MCQ",
                                    questions =
                                        listOf(
                                            Question(
                                                "q2",
                                                2,
                                                "Pick one",
                                                options = listOf(ChoiceOption("A", "a"), ChoiceOption("B", "b")),
                                                acceptedAnswers = listOf("B"),
                                            ),
                                        ),
                                ),
                                QuestionGroup(
                                    id = "g3",
                                    type = QuestionType.SENTENCE_COMPLETION,
                                    instruction = "NO MORE THAN TWO WORDS",
                                    maxWords = 2,
                                    questions =
                                        listOf(
                                            Question(
                                                "q3",
                                                3,
                                                "Gap",
                                                acceptedAnswers = listOf("tax reductions", "tax reduction"),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun scoresNormalizedAnswers() {
        val result = ReadingScorer.score(test, mapOf("q1" to " t ", "q2" to "b", "q3" to "Tax Reductions."))
        assertEquals(3, result.correctCount)
        assertEquals(3, result.totalQuestions)
        assertTrue(result.questionResults.all { it.isCorrect })
    }

    @Test
    fun blankAndTooLongAnswersAreWrong() {
        val result = ReadingScorer.score(test, mapOf("q1" to "", "q3" to "the tax reductions"))
        assertEquals(0, result.correctCount)
        assertEquals(null, result.questionResults.first { it.questionId == "q1" }.userAnswer)
        assertFalse(result.questionResults.first { it.questionId == "q3" }.isCorrect)
    }

    @Test
    fun bundledSampleCanBeFullyAnsweredFromItsKey() {
        val sample = BundledReadingTests.all.first()
        val perfect = sample.allQuestions().associate { it.id to it.acceptedAnswers.first() }
        val result = ReadingScorer.score(sample, perfect)
        assertEquals(sample.questionCount, result.correctCount)
        assertEquals(9.0, result.band)
    }
}

class AnswerNormalizerTest {
    @Test
    fun normalizesCaseSpacingAndPunctuation() {
        assertEquals("the museum", AnswerNormalizer.normalize("  The   Museum. ", QuestionType.SENTENCE_COMPLETION))
    }

    @Test
    fun mapsTfngShortForms() {
        assertEquals("true", AnswerNormalizer.normalize("T", QuestionType.TRUE_FALSE_NOT_GIVEN))
        assertEquals("not given", AnswerNormalizer.normalize("NG", QuestionType.TRUE_FALSE_NOT_GIVEN))
        assertEquals("not given", AnswerNormalizer.normalize("Not  given", QuestionType.TRUE_FALSE_NOT_GIVEN))
        assertEquals("no", AnswerNormalizer.normalize("n", QuestionType.YES_NO_NOT_GIVEN))
    }
}
