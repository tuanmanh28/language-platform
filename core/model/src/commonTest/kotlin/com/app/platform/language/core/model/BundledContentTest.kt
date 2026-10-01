package com.app.platform.language.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

class BundledContentTest {

    @Test
    fun bundledTestsParseAndAreConsistent() {
        val tests = BundledReadingTests.all
        assertTrue(tests.isNotEmpty(), "content/reading phải có ít nhất 1 đề")

        tests.forEach { test ->
            val questions = test.allQuestions()
            assertTrue(questions.isNotEmpty(), "${test.id}: không có câu hỏi")
            assertTrue(
                questions.map { it.id }.toSet().size == questions.size,
                "${test.id}: id câu hỏi bị trùng",
            )
            assertTrue(
                questions.map { it.number } == (1..questions.size).toList(),
                "${test.id}: số thứ tự câu hỏi phải liên tục từ 1",
            )
            questions.forEach { q ->
                assertTrue(q.acceptedAnswers.isNotEmpty(), "${test.id}/${q.id}: thiếu đáp án")
            }
            test.passages.flatMap { it.questionGroups }
                .filter { it.type == QuestionType.MULTIPLE_CHOICE }
                .flatMap { it.questions }
                .forEach { q ->
                    val keys = q.options.map { it.key }
                    assertTrue(q.acceptedAnswers.all { it in keys }, "${test.id}/${q.id}: đáp án không nằm trong options")
                }
        }
    }
}
