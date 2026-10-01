package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.theme.LanguagePlatformTheme

internal enum class TestCardState {
  ENABLED,
  DISABLED,
}

@Composable
internal fun LpTestCard(
  title: String,
  details: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  state: TestCardState = TestCardState.ENABLED,
) {
  Card(onClick = onClick, enabled = state == TestCardState.ENABLED, modifier = modifier) {
    Column(
      modifier = Modifier.padding(LanguagePlatformTheme.spacing.lg),
      verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.xs),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)

      Text(details, style = MaterialTheme.typography.bodyMedium, color = LanguagePlatformTheme.colors.onSurfaceVariant)
    }
  }
}

@Preview
@Composable
private fun LpTestCardPreview() {
  ComponentPreview {
    LpTestCard(
      title = ComponentPreviewData.testSummary.title,
      details = ComponentPreviewData.TEST_DETAILS,
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Preview
@Composable
private fun LpTestCardDisabledPreview() {
  ComponentPreview {
    LpTestCard(
      title = ComponentPreviewData.testSummary.title,
      details = ComponentPreviewData.TEST_DETAILS,
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      state = TestCardState.DISABLED,
    )
  }
}
