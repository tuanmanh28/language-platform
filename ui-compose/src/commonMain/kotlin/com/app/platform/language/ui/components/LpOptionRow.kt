package com.app.platform.language.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_option_label
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import com.app.platform.language.ui.theme.generated.BorderTokens
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LpOptionRow(
  key: String,
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
      modifier = Modifier.padding(end = LanguagePlatformTheme.spacing.lg),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      RadioButton(
        selected = state == AnswerState.SELECTED,
        onClick = null,
        colors = RadioButtonDefaults.colors(selectedColor = colors.border, unselectedColor = colors.border),
        modifier = Modifier.padding(LanguagePlatformTheme.spacing.sm),
      )

      Text(
        text = stringResource(Res.string.common_option_label, key, text),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f).padding(vertical = LanguagePlatformTheme.spacing.sm),
      )

      if (mark != null) Text(mark, style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun OptionRowPreview(state: AnswerState) {
  val option = ComponentPreviewData.choiceQuestion.options.first()

  ComponentPreview {
    LpOptionRow(option.key, option.text, state, onClick = {}, modifier = Modifier.fillMaxWidth())
  }
}

@Preview
@Composable
private fun LpOptionRowIdlePreview() {
  OptionRowPreview(AnswerState.IDLE)
}

@Preview
@Composable
private fun LpOptionRowSelectedPreview() {
  OptionRowPreview(AnswerState.SELECTED)
}

@Preview
@Composable
private fun LpOptionRowCorrectPreview() {
  OptionRowPreview(AnswerState.CORRECT)
}

@Preview
@Composable
private fun LpOptionRowWrongPreview() {
  OptionRowPreview(AnswerState.WRONG)
}
