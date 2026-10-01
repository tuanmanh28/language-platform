package com.app.platform.language.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.platform.language.ui.theme.LanguagePlatformTheme

// Renders a component in the light and the dark theme so every preview checks both modes.
@Composable
internal fun ComponentPreview(content: @Composable () -> Unit) {
  Column {
    ThemedSurface(darkTheme = false, content = content)

    ThemedSurface(darkTheme = true, content = content)
  }
}

@Composable
private fun ThemedSurface(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  LanguagePlatformTheme(darkTheme = darkTheme) {
    Surface {
      Box(Modifier.padding(LanguagePlatformTheme.spacing.lg)) { content() }
    }
  }
}
