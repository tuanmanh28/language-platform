package com.app.platform.language.shared.reading

import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest

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

    fun answerFor(questionId: String): String = answers[questionId].orEmpty()
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
