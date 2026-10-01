package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_close
import com.app.platform.language.ui.resources.common_explanation_title
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LpExplainSheet(
  explanation: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  title: String = stringResource(Res.string.common_explanation_title),
) {
  ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
    ExplainSheetContent(title, explanation, onDismiss, Modifier.fillMaxWidth())
  }
}

@Composable
private fun ExplainSheetContent(
  title: String,
  explanation: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = LanguagePlatformTheme.spacing.xl)
        .padding(bottom = LanguagePlatformTheme.spacing.lg),
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.md),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.semantics { heading() },
    )

    Text(explanation, style = MaterialTheme.typography.bodyLarge)

    LpTextButton(
      text = stringResource(Res.string.common_close),
      onClick = onDismiss,
      modifier = Modifier.align(Alignment.End),
    )
  }
}

// A modal sheet opens in its own window, which static previews cannot show, so this previews the sheet's content.
@Preview
@Composable
private fun LpExplainSheetPreview() {
  ComponentPreview {
    ExplainSheetContent(
      title = stringResource(Res.string.common_explanation_title),
      explanation = ComponentPreviewData.explanation,
      onDismiss = {},
    )
  }
}
