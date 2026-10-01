package com.app.platform.language.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.platform.language.core.model.ReadingTestSummary
import com.app.platform.language.shared.reading.ReadingTestListUiState
import com.app.platform.language.shared.reading.ReadingTestListViewModel
import com.app.platform.language.ui.components.LpPreviews
import com.app.platform.language.ui.components.LpProgressRing
import com.app.platform.language.ui.components.LpTestCard
import com.app.platform.language.ui.components.LpTextButton
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_back
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
  onBack: () -> Unit,
  viewModel: ReadingTestListViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  ReadingTestListScreen(state = state, onRefresh = viewModel::refresh, onOpenTest = onOpenTest, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingTestListScreen(
  state: ReadingTestListUiState,
  onRefresh: () -> Unit,
  onOpenTest: (String) -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.reading_list_title)) },
        navigationIcon = { LpTextButton(text = stringResource(Res.string.common_back), onClick = onBack) },
        actions = { LpTextButton(text = stringResource(Res.string.reading_list_refresh), onClick = onRefresh) },
      )
    },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        ReadingTestListUiState.Loading -> LpProgressRing(progress = null, modifier = Modifier.align(Alignment.Center))
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
    contentPadding = PaddingValues(LanguagePlatformTheme.spacing.lg),
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.md),
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

@Composable
private fun TestCard(
  test: ReadingTestSummary,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LpTestCard(
    title = test.title,
    details =
      stringResource(
        Res.string.reading_list_test_details,
        test.module.label(),
        test.questionCount,
        test.timeLimitMinutes,
      ),
    onClick = onClick,
    modifier = modifier,
  )
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
      modifier = Modifier.padding(LanguagePlatformTheme.spacing.md),
    )
  }
}

@LpPreviews
@Composable
private fun ReadingTestListLoadingPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingTestListUiState.Loading, onRefresh = {}, onOpenTest = {}, onBack = {})
  }
}

@LpPreviews
@Composable
private fun ReadingTestListReadyPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listReady, onRefresh = {}, onOpenTest = {}, onBack = {})
  }
}

@LpPreviews
@Composable
private fun ReadingTestListEmptyPreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listEmpty, onRefresh = {}, onOpenTest = {}, onBack = {})
  }
}

@LpPreviews
@Composable
private fun ReadingTestListOfflinePreview() {
  LanguagePlatformTheme {
    ReadingTestListScreen(state = ReadingPreviewData.listOffline, onRefresh = {}, onOpenTest = {}, onBack = {})
  }
}
