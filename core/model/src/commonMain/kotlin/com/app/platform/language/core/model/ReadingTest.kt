package com.app.platform.language.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Một đề Reading. Cấu trúc khớp với content/schema/reading-test.schema.json.
 * Đề là dữ liệu (không hard-code trong app) nên thêm đề mới không cần phát hành app.
 */
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

    fun allQuestions(): List<Question> =
        passages.flatMap { passage -> passage.questionGroups.flatMap { it.questions } }

    fun toSummary(): ReadingTestSummary = ReadingTestSummary(
        id = id,
        title = title,
        module = module,
        timeLimitMinutes = timeLimitMinutes,
        questionCount = questionCount,
    )
}

@Serializable
enum class IeltsModule {
    @SerialName("academic") ACADEMIC,
    @SerialName("general_training") GENERAL_TRAINING,
}

@Serializable
data class Passage(
    val id: String,
    val title: String,
    val paragraphs: List<Paragraph>,
    val questionGroups: List<QuestionGroup>,
)

/** [label] là A, B, C… dùng cho dạng câu hỏi matching; có thể null. */
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
    /** Giới hạn số từ cho dạng điền từ, vd. "NO MORE THAN TWO WORDS" = 2. */
    val maxWords: Int? = null,
    val questions: List<Question>,
)

@Serializable
enum class QuestionType {
    @SerialName("multiple_choice") MULTIPLE_CHOICE,
    @SerialName("true_false_not_given") TRUE_FALSE_NOT_GIVEN,
    @SerialName("yes_no_not_given") YES_NO_NOT_GIVEN,
    @SerialName("sentence_completion") SENTENCE_COMPLETION;

    /** Các lựa chọn cố định cho dạng TRUE/FALSE/NOT GIVEN và YES/NO/NOT GIVEN; rỗng với dạng khác. */
    val fixedChoices: List<String>
        get() = when (this) {
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
    /** Chỉ dùng cho multiple_choice. */
    val options: List<ChoiceOption> = emptyList(),
    /**
     * Đáp án chấp nhận. Phase 1 gửi kèm về client để chấm offline;
     * khi có tài khoản Premium sẽ chuyển sang chấm ở server.
     */
    val acceptedAnswers: List<String>,
    val explanation: String? = null,
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
