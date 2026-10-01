package com.app.platform.language.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import com.app.platform.language.ui.theme.generated.BorderTokens

@Composable
internal fun LpAnswerChip(
  text: String,
  state: AnswerState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = state.colors()
  val verdict = state.verdict()
  val mark = state.mark()

  Surface(
    selected = state == AnswerState.SELECTED,
    onClick = onClick,
    enabled = !state.isReviewed,
    shape = MaterialTheme.shapes.small,
    color = colors.container,
    contentColor = colors.content,
    border = BorderStroke(BorderTokens.thin, colors.border),
    modifier =
      modifier.semantics {
        role = Role.RadioButton
        if (verdict != null) stateDescription = verdict
      },
  ) {
    Row(
      modifier =
        Modifier.padding(horizontal = LanguagePlatformTheme.spacing.lg, vertical = LanguagePlatformTheme.spacing.sm),
      horizontalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.xs),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (mark != null) Text(mark, style = MaterialTheme.typography.labelLarge)

      Text(text, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Preview
@Composable
private fun LpAnswerChipIdlePreview() {
  ComponentPreview { LpAnswerChip(ComponentPreviewData.fixedChoice, AnswerState.IDLE, onClick = {}) }
}

@Preview
@Composable
private fun LpAnswerChipSelectedPreview() {
  ComponentPreview { LpAnswerChip(ComponentPreviewData.fixedChoice, AnswerState.SELECTED, onClick = {}) }
}

@Preview
@Composable
private fun LpAnswerChipCorrectPreview() {
  ComponentPreview { LpAnswerChip(ComponentPreviewData.fixedChoice, AnswerState.CORRECT, onClick = {}) }
}

@Preview
@Composable
private fun LpAnswerChipWrongPreview() {
  ComponentPreview { LpAnswerChip(ComponentPreviewData.fixedChoice, AnswerState.WRONG, onClick = {}) }
}
