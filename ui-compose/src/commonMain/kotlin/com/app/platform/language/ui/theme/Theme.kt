package com.app.platform.language.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.app.platform.language.ui.theme.generated.ColorTokens
import com.app.platform.language.ui.theme.generated.DarkColorTokens
import com.app.platform.language.ui.theme.generated.LightColorTokens
import com.app.platform.language.ui.theme.generated.RadiusTokens
import com.app.platform.language.ui.theme.generated.SpacingTokens
import com.app.platform.language.ui.theme.generated.TypographyTokens

internal val LocalColors = staticCompositionLocalOf { LightColorTokens }
internal val LocalSpacing = staticCompositionLocalOf { SpacingTokens }
internal val LocalRadius = staticCompositionLocalOf { RadiusTokens }

@Composable
fun LanguagePlatformTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) DarkColorTokens else LightColorTokens
  CompositionLocalProvider(
    LocalColors provides colors,
    LocalSpacing provides SpacingTokens,
    LocalRadius provides RadiusTokens,
  ) {
    MaterialTheme(
      colorScheme = colors.toColorScheme(isDark = darkTheme),
      typography = TokenTypography,
      shapes = TokenShapes,
      content = content,
    )
  }
}

internal object LanguagePlatformTheme {
  val colors: ColorTokens
    @Composable @ReadOnlyComposable
    get() = LocalColors.current

  val spacing: SpacingTokens
    @Composable @ReadOnlyComposable
    get() = LocalSpacing.current

  val radius: RadiusTokens
    @Composable @ReadOnlyComposable
    get() = LocalRadius.current
}

private val TokenTypography =
  Typography(
    displayLarge = TypographyTokens.displayLarge,
    displayMedium = TypographyTokens.displayMedium,
    displaySmall = TypographyTokens.displaySmall,
    headlineLarge = TypographyTokens.headlineLarge,
    headlineMedium = TypographyTokens.headlineMedium,
    headlineSmall = TypographyTokens.headlineSmall,
    titleLarge = TypographyTokens.titleLarge,
    titleMedium = TypographyTokens.titleMedium,
    titleSmall = TypographyTokens.titleSmall,
    bodyLarge = TypographyTokens.bodyLarge,
    bodyMedium = TypographyTokens.bodyMedium,
    bodySmall = TypographyTokens.bodySmall,
    labelLarge = TypographyTokens.labelLarge,
    labelMedium = TypographyTokens.labelMedium,
    labelSmall = TypographyTokens.labelSmall,
  )

private val TokenShapes =
  Shapes(
    extraSmall = RoundedCornerShape(RadiusTokens.xs),
    small = RoundedCornerShape(RadiusTokens.sm),
    medium = RoundedCornerShape(RadiusTokens.md),
    large = RoundedCornerShape(RadiusTokens.lg),
    extraLarge = RoundedCornerShape(RadiusTokens.xl),
  )

private fun ColorTokens.toColorScheme(isDark: Boolean): ColorScheme {
  val baseline = if (isDark) darkColorScheme() else lightColorScheme()
  return baseline.copy(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = primary,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainerLowest = surfaceContainerLowest,
    primaryFixed = primaryFixed,
    primaryFixedDim = primaryFixedDim,
    onPrimaryFixed = onPrimaryFixed,
    onPrimaryFixedVariant = onPrimaryFixedVariant,
    secondaryFixed = secondaryFixed,
    secondaryFixedDim = secondaryFixedDim,
    onSecondaryFixed = onSecondaryFixed,
    onSecondaryFixedVariant = onSecondaryFixedVariant,
    tertiaryFixed = tertiaryFixed,
    tertiaryFixedDim = tertiaryFixedDim,
    onTertiaryFixed = onTertiaryFixed,
    onTertiaryFixedVariant = onTertiaryFixedVariant,
  )
}
