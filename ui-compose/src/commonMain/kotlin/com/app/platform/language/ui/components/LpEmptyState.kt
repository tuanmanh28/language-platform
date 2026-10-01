package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_retry
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpEmptyState(
  title: String,
  modifier: Modifier = Modifier,
  message: String? = null,
  action: (@Composable () -> Unit)? = null,
) {
  Column(
    modifier = modifier.padding(LanguagePlatformTheme.spacing.xl),
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.semantics { heading() },
    )

    if (message != null) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = LanguagePlatformTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }

    if (action != null) action()
  }
}

@Preview
@Composable
private fun LpEmptyStateTitleOnlyPreview() {
  ComponentPreview { LpEmptyState(ComponentPreviewData.EMPTY_TITLE) }
}

@Preview
@Composable
private fun LpEmptyStateWithActionPreview() {
  ComponentPreview {
    LpEmptyState(
      title = ComponentPreviewData.EMPTY_TITLE,
      message = ComponentPreviewData.EMPTY_MESSAGE,
      action = { LpTextButton(stringResource(Res.string.common_retry), onClick = {}) },
    )
  }
}
