package com.app.platform.language.shared.reading

import com.app.platform.language.core.exam.AnswerNormalizer
import com.app.platform.language.core.model.QuestionType
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import kotlin.time.Duration.Companion.minutes

private const val TIME_RUNNING_OUT_SECONDS = 60

// A sealed class, not an interface, so Swift sees nested types such as ReadingSessionUiState.Loading.
sealed class ReadingSessionUiState {
  data object Loading : ReadingSessionUiState()

  data class Failed(
    val error: ReadingError,
  ) : ReadingSessionUiState()

  data class InProgress(
    val test: ReadingTest,
    val answers: Map<String, String>,
    val remainingSeconds: Int,
  ) : ReadingSessionUiState() {
    val answeredCount: Int get() = answers.count { it.value.isNotBlank() }

    // Formatted here so every platform shows the timer the same way.
    val remainingLabel: String get() = formatMinutesAndSeconds(remainingSeconds)

    val remainingFraction: Float
      get() {
        val totalSeconds = test.timeLimitMinutes.minutes.inWholeSeconds
        return if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f) else 0f
      }

    val isTimeRunningOut: Boolean get() = remainingSeconds <= TIME_RUNNING_OUT_SECONDS

    fun answerFor(questionId: String): String = answers[questionId].orEmpty()

    // Counted like the scorer counts, so the word-limit hint matches how the answer is marked.
    fun wordCountFor(questionId: String): Int {
      val type = questionTypeOf(questionId) ?: return 0
      return AnswerNormalizer.wordCount(AnswerNormalizer.normalize(answerFor(questionId), type))
    }

    private fun questionTypeOf(questionId: String): QuestionType? =
      test.passages
        .flatMap { it.questionGroups }
        .firstOrNull { group -> group.questions.any { it.id == questionId } }
        ?.type
  }

  data class Finished(
    val test: ReadingTest,
    val result: ReadingResult,
    val isTimeExpired: Boolean,
  ) : ReadingSessionUiState()
}

private fun formatMinutesAndSeconds(totalSeconds: Int): String {
  val seconds = totalSeconds.coerceAtLeast(0)
  return "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}
