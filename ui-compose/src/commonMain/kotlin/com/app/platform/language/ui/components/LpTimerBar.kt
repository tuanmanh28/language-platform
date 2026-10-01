package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_timer_remaining
import com.app.platform.language.ui.resources.common_timer_running_out
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

internal enum class TimerBarState {
  NORMAL,
  WARNING,
}

@Composable
internal fun LpTimerBar(
  remainingLabel: String,
  progress: Float,
  state: TimerBarState,
  modifier: Modifier = Modifier,
) {
  val colors = LanguagePlatformTheme.colors
  val indicatorColor = if (state == TimerBarState.WARNING) colors.warning else colors.primary
  val labelColor = if (state == TimerBarState.WARNING) colors.warning else colors.onSurface
  val description = stringResource(Res.string.common_timer_remaining, remainingLabel)
  val warning = stringResource(Res.string.common_timer_running_out)

  Column(
    modifier =
      modifier.clearAndSetSemantics {
        contentDescription = description
        if (state == TimerBarState.WARNING) stateDescription = warning
      },
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.xs),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(remainingLabel, style = MaterialTheme.typography.titleMedium, color = labelColor)

      if (state == TimerBarState.WARNING) {
        Text(warning, style = MaterialTheme.typography.labelMedium, color = labelColor)
      }
    }

    LinearProgressIndicator(
      progress = { progress },
      color = indicatorColor,
      trackColor = colors.surfaceContainerHighest,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Preview
@Composable
private fun LpTimerBarNormalPreview() {
  ComponentPreview { LpTimerBar(ComponentPreviewData.TIMER_LABEL, progress = 0.7f, state = TimerBarState.NORMAL) }
}

@Preview
@Composable
private fun LpTimerBarWarningPreview() {
  ComponentPreview {
    LpTimerBar(ComponentPreviewData.TIMER_WARNING_LABEL, progress = 0.01f, state = TimerBarState.WARNING)
  }
}
