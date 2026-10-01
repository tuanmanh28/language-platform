package com.app.platform.language.shared.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.platform.language.shared.reading.data.ReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadingTestListViewModel internal constructor(
  private val repository: ReadingRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<ReadingTestListUiState>(ReadingTestListUiState.Loading)
  val state: StateFlow<ReadingTestListUiState> = _state.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      val catalog = repository.getTests()
      _state.value = ReadingTestListUiState.Ready(catalog.tests, catalog.isOffline)
    }
  }
}
