package com.app.platform.language.shared.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.ReadingResult
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.shared.data.ReadingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class ReadingSessionUiState {
    data object Loading : ReadingSessionUiState()

    data class Error(
        val message: String,
    ) : ReadingSessionUiState()

    data class InProgress(
        val test: ReadingTest,
        /** questionId -> current answer. */
        val answers: Map<String, String>,
        val remainingSeconds: Int,
    ) : ReadingSessionUiState() {
        val answeredCount: Int get() = answers.count { it.value.isNotBlank() }

        /** "mm:ss" — formatted in shared so every platform shows the same thing. */
        val remainingLabel: String get() = formatSeconds(remainingSeconds)

        fun answerFor(questionId: String): String = answers[questionId].orEmpty()
    }

    data class Finished(
        val test: ReadingTest,
        val result: ReadingResult,
        val timeExpired: Boolean,
    ) : ReadingSessionUiState()
}

/**
 * One Reading attempt: loads the test, counts down, keeps answers, scores with [ReadingScorer].
 *
 * Timer lifecycle: the UI calls [start] when the screen appears and [stop] when it disappears
 * (SwiftUI: onAppear/onDisappear, Compose: DisposableEffect).
 */
class ReadingSessionViewModel(
    private val testId: String,
    private val repository: ReadingRepository,
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
        timerJob?.cancel()
        timerJob = null
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

    fun submit() = finish(timeExpired = false)

    /** Starts the test again from scratch (after submitting). */
    fun restart() {
        val test =
            when (val current = _state.value) {
                is ReadingSessionUiState.Finished -> current.test
                is ReadingSessionUiState.InProgress -> current.test
                else -> return
            }
        timerJob?.cancel()
        timerJob = null
        _state.value =
            ReadingSessionUiState.InProgress(
                test = test,
                answers = emptyMap(),
                remainingSeconds = test.timeLimitMinutes * 60,
            )
        startTimerIfNeeded()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ReadingSessionUiState.Loading
            _state.value =
                try {
                    val test = repository.getTest(testId)
                    ReadingSessionUiState.InProgress(
                        test = test,
                        answers = emptyMap(),
                        remainingSeconds = test.timeLimitMinutes * 60,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ReadingSessionUiState.Error(e.message ?: "Không tải được đề")
                }
            startTimerIfNeeded()
        }
    }

    private fun startTimerIfNeeded() {
        if (!isVisible || timerJob?.isActive == true) return
        if (_state.value !is ReadingSessionUiState.InProgress) return

        timerJob =
            viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    val current = _state.value as? ReadingSessionUiState.InProgress ?: break
                    val remaining = current.remainingSeconds - 1
                    if (remaining <= 0) {
                        _state.value = current.copy(remainingSeconds = 0)
                        finish(timeExpired = true)
                        break
                    }
                    _state.value = current.copy(remainingSeconds = remaining)
                }
            }
    }

    private fun finish(timeExpired: Boolean) {
        val current = _state.value as? ReadingSessionUiState.InProgress ?: return
        timerJob?.cancel()
        timerJob = null

        val result = ReadingScorer.score(current.test, current.answers)
        _state.value = ReadingSessionUiState.Finished(current.test, result, timeExpired)

        viewModelScope.launch {
            try {
                repository.saveAttempt(result, current.answers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Could not save attempt" }
            }
        }
    }
}

internal fun formatSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
