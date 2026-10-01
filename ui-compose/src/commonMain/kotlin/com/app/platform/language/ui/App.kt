package com.app.platform.language.ui

import androidx.compose.runtime.Composable
import com.app.platform.language.ui.navigation.AppNavHost
import com.app.platform.language.ui.theme.LanguagePlatformTheme

@Composable
fun LanguagePlatformApp() {
  LanguagePlatformTheme {
    AppNavHost()
  }
}
