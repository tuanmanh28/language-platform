package com.app.platform.language.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundledListeningContentTest {
  private val tests = BundledListeningTests.all

  @Test
  fun bundledListeningTestsExist() {
    assertTrue(tests.isNotEmpty(), "content/listening must contain at least one test")
  }

  @Test
  fun everyTestHasFourNumberedSectionsWithRelativeAudio() {
    tests.forEach { test ->
      assertEquals(listOf(1, 2, 3, 4), test.sections.map { it.number }, "${test.id}: sections must be numbered 1..4")
      test.sections.forEach { section ->
        assertTrue(section.durationSeconds > 0, "${test.id}/${section.id}: duration must be positive")
        assertTrue(
          relativeAudioPath.matches(section.audioUrl),
          "${test.id}/${section.id}: audioUrl must be a path relative to the audio storage",
        )
      }
    }
  }

  @Test
  fun transcriptSegmentsAreOrderedAndWithinTheAudio() {
    tests.flatMap { test -> test.sections.map { "${test.id}/${it.id}" to it } }.forEach { (name, section) ->
      section.transcript.forEach { segment ->
        assertTrue(segment.startSeconds < segment.endSeconds, "$name: segment ends before it starts")
        assertTrue(segment.endSeconds <= section.durationSeconds, "$name: segment ends after the audio")
      }
      section.transcript.zipWithNext().forEach { (previous, next) ->
        assertTrue(previous.endSeconds <= next.startSeconds, "$name: segments overlap or are out of order")
      }
    }
  }

  @Test
  fun questionsAreNumberedConsecutivelyWithAnswerKeys() {
    tests.forEach { test ->
      val questions = test.allQuestions()
      assertTrue(questions.isNotEmpty(), "${test.id}: has no questions")
      assertEquals(questions.size, questions.map { it.id }.toSet().size, "${test.id}: duplicate question ids")
      assertEquals(
        (1..questions.size).toList(),
        questions.map { it.number },
        "${test.id}: question numbers must run consecutively from 1",
      )
      questions.forEach { q ->
        assertTrue(q.acceptedAnswers.isNotEmpty(), "${test.id}/${q.id}: missing answer key")
      }
    }
  }

  @Test
  fun multipleChoiceAnswersAreOptionKeys() {
    tests.forEach { test ->
      test.sections
        .flatMap { it.questionGroups }
        .filter { it.type == QuestionType.MULTIPLE_CHOICE }
        .flatMap { it.questions }
        .forEach { q ->
          val keys = q.options.map { it.key }
          assertTrue(q.acceptedAnswers.all { it in keys }, "${test.id}/${q.id}: answer is not one of the option keys")
        }
    }
  }

  private companion object {
    // Mirrors the audioUrl pattern in content/schema/listening-test.schema.json.
    val relativeAudioPath = Regex("^[a-z0-9][a-z0-9._/-]*$")
  }
}
