package com.app.platform.language.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.app.platform.language.ui.theme.LanguagePlatformTheme

@Immutable
internal data class AnswerColors(
  val container: Color,
  val content: Color,
  val border: Color,
)

@Composable
@ReadOnlyComposable
internal fun AnswerState.colors(): AnswerColors {
  val colors = LanguagePlatformTheme.colors
  return when (this) {
    AnswerState.IDLE -> AnswerColors(colors.surface, colors.onSurface, colors.outline)
    AnswerState.SELECTED -> AnswerColors(colors.primaryContainer, colors.onPrimaryContainer, colors.primary)
    AnswerState.CORRECT -> AnswerColors(colors.successContainer, colors.onSuccessContainer, colors.correct)
    AnswerState.WRONG -> AnswerColors(colors.errorContainer, colors.onErrorContainer, colors.wrong)
  }
}
