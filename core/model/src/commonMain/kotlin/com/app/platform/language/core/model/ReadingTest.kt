package com.app.platform.language.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingTest(
  val id: String,
  val schemaVersion: Int = 1,
  val module: IeltsModule = IeltsModule.ACADEMIC,
  val title: String,
  val timeLimitMinutes: Int,
  val passages: List<Passage>,
) {
  val questionCount: Int
    get() = passages.sumOf { passage -> passage.questionGroups.sumOf { it.questions.size } }

  fun allQuestions(): List<Question> = passages.flatMap { passage -> passage.questionGroups.flatMap { it.questions } }

  fun toSummary(): ReadingTestSummary =
    ReadingTestSummary(
      id = id,
      title = title,
      module = module,
      timeLimitMinutes = timeLimitMinutes,
      questionCount = questionCount,
    )
}

@Serializable
enum class IeltsModule {
  @SerialName("academic")
  ACADEMIC,

  @SerialName("general_training")
  GENERAL_TRAINING,
}

@Serializable
data class Passage(
  val id: String,
  val title: String,
  val paragraphs: List<Paragraph>,
  val questionGroups: List<QuestionGroup>,
)

@Serializable
data class Paragraph(
  val label: String? = null,
  val text: String,
)

@Serializable
data class QuestionGroup(
  val id: String,
  val type: QuestionType,
  val instruction: String,
  val maxWords: Int? = null,
  val questions: List<Question>,
)

@Serializable
enum class QuestionType {
  @SerialName("multiple_choice")
  MULTIPLE_CHOICE,

  @SerialName("true_false_not_given")
  TRUE_FALSE_NOT_GIVEN,

  @SerialName("yes_no_not_given")
  YES_NO_NOT_GIVEN,

  @SerialName("sentence_completion")
  SENTENCE_COMPLETION,

  ;

  val fixedChoices: List<String>
    get() =
      when (this) {
        TRUE_FALSE_NOT_GIVEN -> listOf("TRUE", "FALSE", "NOT GIVEN")
        YES_NO_NOT_GIVEN -> listOf("YES", "NO", "NOT GIVEN")
        MULTIPLE_CHOICE, SENTENCE_COMPLETION -> emptyList()
      }
}

@Serializable
data class Question(
  val id: String,
  val number: Int,
  val prompt: String,
  val options: List<ChoiceOption> = emptyList(),
  // Shipped in test payloads so the apps score and explain offline.
  val acceptedAnswers: List<String>,
  @Serializable(with = LegacyExplanationSerializer::class)
  val explanation: Explanation? = null,
)

@Serializable
data class ChoiceOption(
  val key: String,
  val text: String,
)

@Serializable
data class ReadingTestSummary(
  val id: String,
  val title: String,
  val module: IeltsModule,
  val timeLimitMinutes: Int,
  val questionCount: Int,
)
