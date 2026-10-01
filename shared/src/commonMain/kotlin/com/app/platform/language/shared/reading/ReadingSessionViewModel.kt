package com.app.platform.language.shared.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.shared.reading.data.ReadingRepository
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ReadingSessionViewModel internal constructor(
  private val testId: String,
  private val repository: ReadingRepository,
  private val clock: Clock,
) : ViewModel() {
  private val log = Logger.withTag("ReadingSession")
  private val _state = MutableStateFlow<ReadingSessionUiState>(ReadingSessionUiState.Loading)
  val state: StateFlow<ReadingSessionUiState> = _state.asStateFlow()

  private var timerJob: Job? = null
  private var isVisible = false

  init {
    load()
  }

  fun retry() = load()

  fun start() {
    isVisible = true
    startTimerIfNeeded()
  }

  fun stop() {
    isVisible = false
    stopTimer()
  }

  fun answer(
    questionId: String,
    value: String,
  ) {
    _state.update { current ->
      if (current is ReadingSessionUiState.InProgress) {
        current.copy(answers = current.answers + (questionId to value))
      } else {
        current
      }
    }
  }

  fun submit() = finish(isTimeExpired = false)

  fun restart() {
    val test =
      when (val current = _state.value) {
        is ReadingSessionUiState.Finished -> current.test
        is ReadingSessionUiState.InProgress -> current.test
        ReadingSessionUiState.Loading, is ReadingSessionUiState.Failed -> return
      }
    begin(test)
  }

  private fun load() {
    viewModelScope.launch {
      _state.value = ReadingSessionUiState.Loading
      repository.getTest(testId).mapBoth(
        success = { test -> begin(test) },
        failure = { error -> _state.value = ReadingSessionUiState.Failed(error) },
      )
    }
  }

  private fun begin(test: ReadingTest) {
    stopTimer()
    val timeLimit = test.timeLimitMinutes.minutes
    _state.value =
      ReadingSessionUiState.InProgress(
        test = test,
        answers = emptyMap(),
        remainingSeconds = timeLimit.inWholeSeconds.toInt(),
      )
    startTimerIfNeeded()
  }

  // The deadline is re-derived on every start so the countdown pauses while the screen is hidden.
  private fun startTimerIfNeeded() {
    if (!isVisible || timerJob != null) return
    val current = _state.value as? ReadingSessionUiState.InProgress ?: return
    val deadline = clock.now() + current.remainingSeconds.seconds
    timerJob =
      viewModelScope.launch {
        do {
          delay(1.seconds)
          // Rounds up because delay() resumes slightly late, which would otherwise skip a second.
          val remainingSeconds = ceil((deadline - clock.now()) / 1.seconds).toInt().coerceAtLeast(0)
          _state.update { state ->
            (state as? ReadingSessionUiState.InProgress)?.copy(remainingSeconds = remainingSeconds) ?: state
          }
        } while (remainingSeconds > 0)
        finish(isTimeExpired = true)
      }
  }

  private fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }

  private fun finish(isTimeExpired: Boolean) {
    val current = _state.value as? ReadingSessionUiState.InProgress ?: return
    stopTimer()

    val result = ReadingScorer.score(current.test, current.answers)
    _state.value = ReadingSessionUiState.Finished(current.test, result, isTimeExpired)
    viewModelScope.launch {
      repository
        .saveAttempt(result, current.answers)
        .onErr { error -> log.w { "Saving attempt for ${result.testId} failed ($error)" } }
    }
  }
}
