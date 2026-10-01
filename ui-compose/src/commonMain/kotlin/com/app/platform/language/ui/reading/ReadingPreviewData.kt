package com.app.platform.language.ui.reading

import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.shared.reading.ReadingTestListUiState

internal object ReadingPreviewData {
  private val test = BundledReadingTests.all.first()
  private val firstQuestion = test.allQuestions().first()
  private val answers = mapOf(firstQuestion.id to firstQuestion.acceptedAnswers.first())

  val listReady = ReadingTestListUiState.Ready(BundledReadingTests.all.map { it.toSummary() }, isOffline = false)
  val listOffline = listReady.copy(isOffline = true)
  val listEmpty = listReady.copy(tests = emptyList())

  val sessionFailed = ReadingSessionUiState.Failed(ReadingError.Offline)
  val sessionInProgress = ReadingSessionUiState.InProgress(test, answers, remainingSeconds = 45)
  val sessionFinished = ReadingSessionUiState.Finished(test, ReadingScorer.score(test, answers), isTimeExpired = true)
}
