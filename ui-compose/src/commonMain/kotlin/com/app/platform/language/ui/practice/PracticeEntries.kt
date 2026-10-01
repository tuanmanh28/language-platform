package com.app.platform.language.ui.practice

import androidx.navigation3.runtime.EntryProviderScope
import com.app.platform.language.ui.navigation.AppRoute
import com.app.platform.language.ui.navigation.Navigator
import com.app.platform.language.ui.navigation.PracticeRoute
import com.app.platform.language.ui.navigation.ReadingTestListRoute

internal fun EntryProviderScope<AppRoute>.practiceEntries(navigator: Navigator) {
  entry<PracticeRoute> {
    PracticeHomeScreen(onOpenReading = { navigator.add(ReadingTestListRoute) })
  }
}
