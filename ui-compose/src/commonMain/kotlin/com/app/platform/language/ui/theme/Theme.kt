package com.app.platform.language.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder colors until the design system defines real tokens.
private val Brand = Color(0xFF2457C5)
private val BrandDark = Color(0xFFB3C5FF)

private val LightColors =
  lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF00687A),
    error = Color(0xFFBA1A1A),
  )

private val DarkColors =
  darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF003EA8),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFF55D6F4),
    error = Color(0xFFFFB4AB),
  )

object ResultColors {
  val correct = Color(0xFF1B7F3B)
  val wrong = Color(0xFFBA1A1A)
}

@Composable
fun LanguagePlatformTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    content = content,
  )
}
