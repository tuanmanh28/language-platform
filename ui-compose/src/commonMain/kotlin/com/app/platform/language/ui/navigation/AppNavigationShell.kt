package com.app.platform.language.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val NavigationRailMinWidth = 600.dp

@Composable
internal fun AppNavigationShell(
  selectedRoute: TopLevelRoute,
  onSelectTab: (TopLevelRoute) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  BoxWithConstraints(modifier.fillMaxSize()) {
    val isWide = maxWidth >= NavigationRailMinWidth

    Scaffold(
      bottomBar = {
        if (!isWide) TabBar(selectedRoute, onSelectTab)
      },
    ) { padding ->
      Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
        if (isWide) TabRail(selectedRoute, onSelectTab, Modifier.fillMaxHeight())

        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
      }
    }
  }
}

@Composable
private fun TabBar(
  selectedRoute: TopLevelRoute,
  onSelectTab: (TopLevelRoute) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(modifier = modifier) {
    TopLevelTab.entries.forEach { tab ->
      NavigationBarItem(
        selected = tab.route == selectedRoute,
        onClick = { onSelectTab(tab.route) },
        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
        label = { Text(stringResource(tab.label)) },
      )
    }
  }
}

@Composable
private fun TabRail(
  selectedRoute: TopLevelRoute,
  onSelectTab: (TopLevelRoute) -> Unit,
  modifier: Modifier = Modifier,
) {
  // The surrounding Scaffold already applies the system bar insets.
  NavigationRail(modifier = modifier, windowInsets = WindowInsets(0)) {
    TopLevelTab.entries.forEach { tab ->
      NavigationRailItem(
        selected = tab.route == selectedRoute,
        onClick = { onSelectTab(tab.route) },
        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
        label = { Text(stringResource(tab.label)) },
      )
    }
  }
}

@Preview
@Composable
private fun AppNavigationShellPreview() {
  LanguagePlatformTheme {
    AppNavigationShell(selectedRoute = PracticeRoute, onSelectTab = {}) {}
  }
}

@Preview
@Composable
private fun AppNavigationShellDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    AppNavigationShell(selectedRoute = VocabularyRoute, onSelectTab = {}) {}
  }
}
