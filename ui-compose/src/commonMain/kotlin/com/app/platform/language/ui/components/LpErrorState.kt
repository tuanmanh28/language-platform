package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_retry
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpErrorState(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(LanguagePlatformTheme.spacing.xl),
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.md, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )

    LpPrimaryButton(text = stringResource(Res.string.common_retry), onClick = onRetry)
  }
}

@Preview
@Composable
private fun LpErrorStatePreview() {
  ComponentPreview { LpErrorState(ComponentPreviewData.ERROR_MESSAGE, onRetry = {}) }
}
