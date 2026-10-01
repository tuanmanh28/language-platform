package com.app.platform.language.ui.reading

import androidx.navigation3.runtime.EntryProviderScope
import com.app.platform.language.ui.navigation.AppRoute
import com.app.platform.language.ui.navigation.Navigator
import com.app.platform.language.ui.navigation.ReadingSessionRoute
import com.app.platform.language.ui.navigation.ReadingTestListRoute

internal fun EntryProviderScope<AppRoute>.readingEntries(navigator: Navigator) {
  entry<ReadingTestListRoute> {
    ReadingTestListScreen(
      onOpenTest = { testId -> navigator.add(ReadingSessionRoute(testId)) },
      onBack = navigator::goBack,
    )
  }

  entry<ReadingSessionRoute> { route ->
    ReadingSessionScreen(testId = route.testId, onExit = navigator::goBack)
  }
}
