package com.app.platform.language.backend.content

import com.app.platform.language.core.model.ListeningTest
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.ReadingTest

fun ReadingTest.withoutAnswerKey(): ReadingTest =
  copy(passages = passages.map { it.copy(questionGroups = it.questionGroups.withoutAnswerKey()) })

fun ListeningTest.withoutAnswerKey(): ListeningTest =
  copy(sections = sections.map { it.copy(questionGroups = it.questionGroups.withoutAnswerKey()) })

private fun List<QuestionGroup>.withoutAnswerKey(): List<QuestionGroup> =
  map { group ->
    group.copy(questions = group.questions.map { it.copy(acceptedAnswers = emptyList(), explanation = null) })
  }
