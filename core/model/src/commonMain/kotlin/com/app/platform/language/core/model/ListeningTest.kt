package com.app.platform.language.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ListeningTest(
  val id: String,
  val schemaVersion: Int = 1,
  val title: String,
  val sections: List<ListeningSection>,
) {
  val questionCount: Int
    get() = sections.sumOf { section -> section.questionGroups.sumOf { it.questions.size } }

  val durationSeconds: Int
    get() = sections.sumOf { it.durationSeconds }

  fun allQuestions(): List<Question> = sections.flatMap { section -> section.questionGroups.flatMap { it.questions } }

  fun toSummary(): ListeningTestSummary =
    ListeningTestSummary(
      id = id,
      title = title,
      durationSeconds = durationSeconds,
      questionCount = questionCount,
    )
}

@Serializable
data class ListeningSection(
  val id: String,
  val number: Int,
  val title: String,
  // A path relative to the audio storage in content files; the API turns it into an absolute URL.
  val audioUrl: String,
  val durationSeconds: Int,
  val transcript: List<TranscriptSegment> = emptyList(),
  val questionGroups: List<QuestionGroup>,
)

@Serializable
data class TranscriptSegment(
  val startSeconds: Int,
  val endSeconds: Int,
  val speaker: String? = null,
  val text: String,
)

@Serializable
data class ListeningTestSummary(
  val id: String,
  val title: String,
  val durationSeconds: Int,
  val questionCount: Int,
)
