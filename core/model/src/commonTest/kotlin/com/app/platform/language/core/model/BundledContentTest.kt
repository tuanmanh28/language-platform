package com.app.platform.language.core.model

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledContentTest {
  @Test
  fun bundledTestsParseAndAreConsistent() {
    val tests = BundledReadingTests.all
    assertTrue(tests.isNotEmpty(), "content/reading must contain at least one test")

    tests.forEach { test ->
      val questions = test.allQuestions()
      assertTrue(questions.isNotEmpty(), "${test.id}: has no questions")
      assertTrue(
        questions.map { it.id }.toSet().size == questions.size,
        "${test.id}: duplicate question ids",
      )
      assertTrue(
        questions.map { it.number } == (1..questions.size).toList(),
        "${test.id}: question numbers must run consecutively from 1",
      )
      questions.forEach { q ->
        assertTrue(q.acceptedAnswers.isNotEmpty(), "${test.id}/${q.id}: missing answer key")
      }
      test.passages
        .flatMap { it.questionGroups }
        .filter { it.type == QuestionType.MULTIPLE_CHOICE }
        .flatMap { it.questions }
        .forEach { q ->
          val keys = q.options.map { it.key }
          assertTrue(
            q.acceptedAnswers.all { it in keys },
            "${test.id}/${q.id}: answer is not one of the option keys",
          )
        }
    }
  }

  @Test
  fun everyQuestionIsExplainedWithAQuoteFromItsPassage() {
    BundledReadingTests.all.forEach { test ->
      test.passages.forEach { passage ->
        passage.questionGroups.flatMap { it.questions }.forEach { question ->
          val name = "${test.id}/${question.id}"
          val explanation = assertExplained(name, question)
          val evidence = assertIs<PassageEvidence>(explanation.evidence, "$name: needs passage evidence")
          val paragraph =
            assertNotNull(
              passage.paragraphs.firstOrNull { it.label == evidence.paragraphId },
              "$name: paragraph ${evidence.paragraphId} is not in passage ${passage.id}",
            )
          assertTrue(evidence.quote in paragraph.text, "$name: quote is not verbatim in paragraph ${paragraph.label}")
          assertParaphrasesMatch(name, question, explanation, source = paragraph.text)
        }
      }
    }
  }
}
