package com.app.platform.language.shared.reading

import app.cash.turbine.test
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.shared.reading.fake.FakeReadingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingTestListViewModelTest {
  private val repository = FakeReadingRepository()
  private val dispatcher = StandardTestDispatcher()
  private val sample = BundledReadingTests.all.first()

  @BeforeTest
  fun setUp() = Dispatchers.setMain(dispatcher)

  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun testsAreShownAfterLoading() =
    runTest(dispatcher) {
      repository.tests = listOf(sample)
      val viewModel = ReadingTestListViewModel(repository)

      viewModel.state.test {
        assertEquals(ReadingTestListUiState.Loading, awaitItem())
        assertEquals(ReadingTestListUiState.Ready(listOf(sample.toSummary()), isOffline = false), awaitItem())
      }
    }

  @Test
  fun offlineCatalogIsFlaggedOffline() =
    runTest(dispatcher) {
      repository.tests = listOf(sample)
      repository.isOffline = true
      val viewModel = ReadingTestListViewModel(repository)

      viewModel.state.test {
        assertEquals(ReadingTestListUiState.Loading, awaitItem())
        assertEquals(ReadingTestListUiState.Ready(listOf(sample.toSummary()), isOffline = true), awaitItem())
      }
    }

  @Test
  fun refreshReplacesTheShownTests() =
    runTest(dispatcher) {
      val viewModel = ReadingTestListViewModel(repository)

      viewModel.state.test {
        assertEquals(ReadingTestListUiState.Loading, awaitItem())
        assertEquals(ReadingTestListUiState.Ready(emptyList(), isOffline = false), awaitItem())

        repository.tests = listOf(sample)
        viewModel.refresh()

        assertEquals(ReadingTestListUiState.Ready(listOf(sample.toSummary()), isOffline = false), awaitItem())
      }
    }
}
