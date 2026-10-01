package com.app.platform.language.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.reading.ReadingTestListUiState
import com.app.platform.language.shared.reading.ReadingTestListViewModel
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.reading_list_offline_banner
import com.app.platform.language.ui.resources.reading_list_refresh
import com.app.platform.language.ui.resources.reading_list_section_title
import com.app.platform.language.ui.resources.reading_list_test_details
import com.app.platform.language.ui.resources.reading_list_title
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReadingTestListScreen(
  onOpenTest: (String) -> Unit,
  viewModel: ReadingTestListViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  ReadingTestListScreen(state = state, onRefresh = viewModel::refresh, onOpenTest = onOpenTest)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingTestListScreen(
  state: ReadingTestListUiState,
  onRefresh: () -> Unit,
  onOpenTest: (String) -> Unit,
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.reading_list_title)) },
        actions = { TextButton(onClick = onRefresh) { Text(stringResource(Res.string.reading_list_refresh)) } },
      )
    },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        ReadingTestListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        is ReadingTestListUiState.Ready -> ReadyContent(state, onOpenTest, Modifier.fillMaxSize())
      }
    }
  }
}

@Composable
private fun ReadyContent(
  state: ReadingTestListUiState.Ready,
  onOpenTest: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.isOffline) {
      item(key = "offline-banner") { OfflineBanner(Modifier.fillMaxWidth()) }
    }

    item(key = "section-title") {
      Text(stringResource(Res.string.reading_list_section_title), style = MaterialTheme.typography.titleSmall)
    }

    items(state.tests, key = { it.id }) { test ->
      TestCard(test = test, onClick = { onOpenTest(test.id) }, modifier = Modifier.fillMaxWidth())
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestCard(
  test: ReadingTestSummary,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(onClick = onClick, modifier = modifier) {
    Column(Modifier.padding(16.dp)) {
      Text(test.title, style = MaterialTheme.typography.titleMedium)

      Spacer(Modifier.height(4.dp))

      Text(
        text =
          stringResource(
            Res.string.reading_list_test_details,
            test.module.label(),
            test.questionCount,
            test.timeLimitMinutes,
          ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    Text(
      text = stringResource(Res.string.reading_list_offline_banner),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(12.dp),
    )
  }
}

@Preview
@Composable
private fun ReadingTestListLoadingPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingTestListUiState.Loading, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListLoadingDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingTestListScreen(state = ReadingTestListUiState.Loading, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListReadyPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listReady, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListReadyDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingTestListScreen(state = ReadingPreviewData.listReady, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListEmptyPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listEmpty, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListEmptyDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingTestListScreen(state = ReadingPreviewData.listEmpty, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListOfflinePreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listOffline, onRefresh = {}, onOpenTest = {})
  }
}

@Preview
@Composable
private fun ReadingTestListOfflineDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingTestListScreen(state = ReadingPreviewData.listOffline, onRefresh = {}, onOpenTest = {})
  }
}
