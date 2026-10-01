package com.app.platform.language.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppNavigationShellTest {
  @Test
  fun clickingTabSelectsIt() =
    runComposeUiTest {
      setContent {
        var selectedRoute by remember { mutableStateOf<TopLevelRoute>(PracticeRoute) }

        LanguagePlatformTheme {
          AppNavigationShell(selectedRoute = selectedRoute, onSelectTab = { selectedRoute = it }) {}
        }
      }

      onNodeWithText("Luyện tập").assertIsSelected()

      onNodeWithText("Từ vựng").performClick()

      onNodeWithText("Từ vựng").assertIsSelected()
      onNodeWithText("Luyện tập").assertIsNotSelected()
    }
}
