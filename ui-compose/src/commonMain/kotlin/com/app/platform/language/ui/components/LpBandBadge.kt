package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_band
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

internal enum class BandBadgeSize {
  SMALL,
  LARGE,
}

@Composable
internal fun LpBandBadge(
  band: Double,
  modifier: Modifier = Modifier,
  size: BandBadgeSize = BandBadgeSize.SMALL,
) {
  val label = band.toString()
  val description = stringResource(Res.string.common_band, label)
  val spacing = LanguagePlatformTheme.spacing
  val isLarge = size == BandBadgeSize.LARGE

  Surface(
    color = LanguagePlatformTheme.colors.primaryContainer,
    contentColor = LanguagePlatformTheme.colors.onPrimaryContainer,
    shape = if (isLarge) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.small,
    modifier = modifier.clearAndSetSemantics { contentDescription = description },
  ) {
    Text(
      text = label,
      style = if (isLarge) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleMedium,
      modifier =
        Modifier.padding(
          horizontal = if (isLarge) spacing.xl else spacing.sm,
          vertical = if (isLarge) spacing.sm else spacing.xs,
        ),
    )
  }
}

@Preview
@Composable
private fun LpBandBadgeSmallPreview() {
  ComponentPreview { LpBandBadge(ComponentPreviewData.BAND) }
}

@Preview
@Composable
private fun LpBandBadgeLargePreview() {
  ComponentPreview { LpBandBadge(ComponentPreviewData.BAND, size = BandBadgeSize.LARGE) }
}
