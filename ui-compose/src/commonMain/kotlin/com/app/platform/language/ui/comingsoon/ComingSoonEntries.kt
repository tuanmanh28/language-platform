package com.app.platform.language.ui.comingsoon

import androidx.navigation3.runtime.EntryProviderScope
import com.app.platform.language.ui.navigation.AppRoute
import com.app.platform.language.ui.navigation.ProfileRoute
import com.app.platform.language.ui.navigation.ProgressRoute
import com.app.platform.language.ui.navigation.VocabularyRoute
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.navigation_tab_profile
import com.app.platform.language.ui.resources.navigation_tab_progress
import com.app.platform.language.ui.resources.navigation_tab_vocabulary

internal fun EntryProviderScope<AppRoute>.comingSoonEntries() {
  entry<VocabularyRoute> { ComingSoonScreen(title = Res.string.navigation_tab_vocabulary) }

  entry<ProgressRoute> { ComingSoonScreen(title = Res.string.navigation_tab_progress) }

  entry<ProfileRoute> { ComingSoonScreen(title = Res.string.navigation_tab_profile) }
}
