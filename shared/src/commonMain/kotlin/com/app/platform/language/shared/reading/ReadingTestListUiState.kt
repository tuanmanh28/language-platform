package com.app.platform.language.shared.reading

import com.app.platform.language.core.model.ReadingTestSummary

// A sealed class, not an interface, so Swift sees nested types such as ReadingTestListUiState.Loading.
sealed class ReadingTestListUiState {
  data object Loading : ReadingTestListUiState()

  data class Ready(
    val tests: List<ReadingTestSummary>,
    val isOffline: Boolean,
  ) : ReadingTestListUiState()
}
