package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_loading
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import com.app.platform.language.ui.theme.generated.SizeTokens
import org.jetbrains.compose.resources.stringResource

internal enum class ProgressRingSize(
  val diameter: Dp,
  val strokeWidth: Dp,
) {
  SMALL(diameter = SizeTokens.progressRingSmall, strokeWidth = SizeTokens.progressRingStrokeSmall),
  LARGE(diameter = SizeTokens.progressRingLarge, strokeWidth = SizeTokens.progressRingStrokeLarge),
}

// A null progress means the amount of work is unknown, so the ring spins.
@Composable
internal fun LpProgressRing(
  progress: Float?,
  modifier: Modifier = Modifier,
  size: ProgressRingSize = ProgressRingSize.SMALL,
  label: String? = null,
) {
  val colors = LanguagePlatformTheme.colors
  val loading = stringResource(Res.string.common_loading)

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (progress == null) {
      CircularProgressIndicator(
        color = colors.primary,
        strokeWidth = size.strokeWidth,
        modifier = Modifier.size(size.diameter).semantics { contentDescription = loading },
      )
    } else {
      CircularProgressIndicator(
        progress = { progress },
        color = colors.primary,
        trackColor = colors.surfaceContainerHighest,
        strokeWidth = size.strokeWidth,
        modifier = Modifier.size(size.diameter),
      )
    }

    if (label != null) Text(label, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
  }
}

@Preview
@Composable
private fun LpProgressRingIndeterminatePreview() {
  ComponentPreview { LpProgressRing(progress = null) }
}

@Preview
@Composable
private fun LpProgressRingDeterminatePreview() {
  ComponentPreview {
    LpProgressRing(progress = 0.65f, size = ProgressRingSize.LARGE, label = ComponentPreviewData.BAND.toString())
  }
}
