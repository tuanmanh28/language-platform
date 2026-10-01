package com.app.platform.language.shared.reading

import app.cash.turbine.test
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.shared.reading.fake.FakeReadingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingSessionViewModelTest {
  private val repository = FakeReadingRepository()
  private val dispatcher = StandardTestDispatcher()
  private val clock =
    object : Clock {
      override fun now(): Instant = Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
    }
  private val lateClock =
    object : Clock {
      override fun now(): Instant {
        val virtualTime = dispatcher.scheduler.currentTime
        return Instant.fromEpochMilliseconds(if (virtualTime == 0L) 0 else virtualTime + TIMER_LATENESS_MS)
      }
    }
  private val sample = BundledReadingTests.all.first().copy(timeLimitMinutes = 1)

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository.tests = listOf(sample)
  }

  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private fun createViewModel(
    testId: String = sample.id,
    clock: Clock = this.clock,
  ) = ReadingSessionViewModel(testId, repository, clock)

  @Test
  fun loadedTestStartsWithTheFullTimeLimit() =
    runTest(dispatcher) {
      createViewModel().state.test {
        assertEquals(ReadingSessionUiState.Loading, awaitItem())
        assertEquals(ReadingSessionUiState.InProgress(sample, emptyMap(), remainingSeconds = 60), awaitItem())
      }
    }

  @Test
  fun missingTestShowsNotFound() =
    runTest(dispatcher) {
      createViewModel(testId = "missing").state.test {
        assertEquals(ReadingSessionUiState.Loading, awaitItem())
        assertEquals(ReadingSessionUiState.Failed(ReadingError.NotFound), awaitItem())
      }
    }

  @Test
  fun retryAfterFailureLoadsTheTest() =
    runTest(dispatcher) {
      repository.nextTestError = ReadingError.Offline
      val viewModel = createViewModel()

      viewModel.state.test {
        assertEquals(ReadingSessionUiState.Loading, awaitItem())
        assertEquals(ReadingSessionUiState.Failed(ReadingError.Offline), awaitItem())

        repository.nextTestError = null
        viewModel.retry()

        assertEquals(ReadingSessionUiState.Loading, awaitItem())
        assertIs<ReadingSessionUiState.InProgress>(awaitItem())
      }
    }

  @Test
  fun timerDoesNotRunBeforeTheScreenStarts() =
    runTest(dispatcher) {
      createViewModel().state.test {
        skipItems(2)
        advanceTimeBy(10.seconds)
        runCurrent()

        expectNoEvents()
      }
    }

  @Test
  fun timerCountsDownEverySecond() =
    runTest(dispatcher) {
      createViewModel().apply { start() }.state.test {
        skipItems(2)
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(59, assertIs<ReadingSessionUiState.InProgress>(awaitItem()).remainingSeconds)
      }
    }

  @Test
  fun lateTimerTickStillCountsDownOneSecond() =
    runTest(dispatcher) {
      createViewModel(clock = lateClock).apply { start() }.state.test {
        skipItems(2)
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(59, assertIs<ReadingSessionUiState.InProgress>(awaitItem()).remainingSeconds)
      }
    }

  @Test
  fun lateTimerTicksExpireOnlyAtTheFullTimeLimit() =
    runTest(dispatcher) {
      createViewModel(clock = lateClock).apply { start() }.state.test {
        advanceTimeBy(59.seconds)
        runCurrent()
        assertEquals(1, assertIs<ReadingSessionUiState.InProgress>(expectMostRecentItem()).remainingSeconds)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertTrue(assertIs<ReadingSessionUiState.Finished>(expectMostRecentItem()).isTimeExpired)
      }
    }

  @Test
  fun stoppedScreenPausesTheTimer() =
    runTest(dispatcher) {
      val viewModel = createViewModel().apply { start() }

      viewModel.state.test {
        skipItems(2)
        advanceTimeBy(1.seconds)
        runCurrent()
        skipItems(1)

        viewModel.stop()
        advanceTimeBy(10.seconds)
        runCurrent()
        expectNoEvents()

        viewModel.start()
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(58, assertIs<ReadingSessionUiState.InProgress>(awaitItem()).remainingSeconds)
      }
    }

  @Test
  fun expiredTimerSubmitsAutomatically() =
    runTest(dispatcher) {
      createViewModel().apply { start() }.state.test {
        advanceTimeBy(1.minutes)
        runCurrent()

        val finished = assertIs<ReadingSessionUiState.Finished>(expectMostRecentItem())
        assertTrue(finished.isTimeExpired)
        assertEquals(listOf(finished.result), repository.savedAttempts)
      }
    }

  @Test
  fun failedAttemptSaveKeepsTheResult() =
    runTest(dispatcher) {
      repository.nextSaveError = ReadingError.Unexpected(IllegalStateException("disk full"))
      val viewModel = createViewModel()

      viewModel.state.test {
        skipItems(2)
        viewModel.submit()
        assertIs<ReadingSessionUiState.Finished>(awaitItem())
        runCurrent()

        expectNoEvents()
        assertTrue(repository.savedAttempts.isEmpty())
      }
    }

  @Test
  fun submitScoresAnswersAndSavesTheAttempt() =
    runTest(dispatcher) {
      val viewModel = createViewModel()
      val question = sample.allQuestions().first()

      viewModel.state.test {
        skipItems(2)
        viewModel.answer(question.id, question.acceptedAnswers.first())
        skipItems(1)
        viewModel.submit()

        val finished = assertIs<ReadingSessionUiState.Finished>(awaitItem())
        assertFalse(finished.isTimeExpired)
        assertEquals(1, finished.result.correctCount)
        runCurrent()
        assertEquals(listOf(finished.result), repository.savedAttempts)
      }
    }

  @Test
  fun restartClearsAnswersAndResetsTheTimer() =
    runTest(dispatcher) {
      val viewModel = createViewModel()

      viewModel.state.test {
        skipItems(2)
        viewModel.submit()
        skipItems(1)
        viewModel.restart()

        assertEquals(ReadingSessionUiState.InProgress(sample, emptyMap(), remainingSeconds = 60), awaitItem())
      }
    }
}

private const val TIMER_LATENESS_MS = 5L
