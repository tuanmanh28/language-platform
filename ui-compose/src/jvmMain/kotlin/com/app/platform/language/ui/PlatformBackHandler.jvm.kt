package com.app.platform.language.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(
  enabled: Boolean,
  onBack: () -> Unit,
) {
  // Desktop has no system Back button.
}
