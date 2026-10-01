package com.app.platform.language.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.app.platform.language.ui.comingsoon.comingSoonEntries
import com.app.platform.language.ui.practice.practiceEntries
import com.app.platform.language.ui.reading.readingEntries

@Composable
internal fun AppNavHost(modifier: Modifier = Modifier) {
  val navState = rememberNavState(startRoute = PracticeRoute, topLevelRoutes = TopLevelTab.entries.map { it.route })
  val navigator = remember(navState) { Navigator(navState) }
  val entryProvider =
    entryProvider {
      practiceEntries(navigator)
      readingEntries(navigator)
      comingSoonEntries()
    }

  AppNavigationShell(
    selectedRoute = navState.topLevelRoute,
    onSelectTab = navigator::selectTab,
    modifier = modifier,
  ) {
    NavDisplay(
      entries = navState.toDecoratedEntries(entryProvider),
      onBack = navigator::goBack,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

// Every tab keeps its own decorated entries, so switching tabs preserves each tab's screens and ViewModels.
@Composable
private fun NavState.toDecoratedEntries(entryProvider: (AppRoute) -> NavEntry<AppRoute>): List<NavEntry<AppRoute>> {
  val entriesByTab =
    backStacks.mapValues { (_, backStack) ->
      val decorators =
        listOf(
          rememberSaveableStateHolderNavEntryDecorator<AppRoute>(),
          rememberViewModelStoreNavEntryDecorator(),
        )
      rememberDecoratedNavEntries(backStack = backStack, entryDecorators = decorators, entryProvider = entryProvider)
    }

  return stacksInUse.flatMap { entriesByTab.getValue(it) }
}
