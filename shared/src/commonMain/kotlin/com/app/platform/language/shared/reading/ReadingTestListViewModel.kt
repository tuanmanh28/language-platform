package com.app.platform.language.shared.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.data.DataSource
import com.app.platform.language.shared.data.ReadingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReadingTestListUiState {
    data object Loading : ReadingTestListUiState()
    data class Error(val message: String) : ReadingTestListUiState()
    data class Success(
        val tests: List<ReadingTestSummary>,
        /** true khi đang dùng cache/đề nhúng sẵn vì không gọi được server. */
        val isOffline: Boolean,
    ) : ReadingTestListUiState()
}

class ReadingTestListViewModel(
    private val repository: ReadingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReadingTestListUiState>(ReadingTestListUiState.Loading)
    val state: StateFlow<ReadingTestListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_state.value !is ReadingTestListUiState.Success) {
                _state.value = ReadingTestListUiState.Loading
            }
            _state.value = try {
                val result = repository.loadTests()
                ReadingTestListUiState.Success(
                    tests = result.tests,
                    isOffline = result.source != DataSource.NETWORK,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReadingTestListUiState.Error(e.message ?: "Đã có lỗi xảy ra")
            }
        }
    }
}
