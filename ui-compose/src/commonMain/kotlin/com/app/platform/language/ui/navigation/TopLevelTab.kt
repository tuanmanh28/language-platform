package com.app.platform.language.ui.navigation

import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.ic_tab_practice
import com.app.platform.language.ui.resources.ic_tab_profile
import com.app.platform.language.ui.resources.ic_tab_progress
import com.app.platform.language.ui.resources.ic_tab_vocabulary
import com.app.platform.language.ui.resources.navigation_tab_practice
import com.app.platform.language.ui.resources.navigation_tab_profile
import com.app.platform.language.ui.resources.navigation_tab_progress
import com.app.platform.language.ui.resources.navigation_tab_vocabulary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal enum class TopLevelTab(
  val route: TopLevelRoute,
  val label: StringResource,
  val icon: DrawableResource,
) {
  PRACTICE(PracticeRoute, Res.string.navigation_tab_practice, Res.drawable.ic_tab_practice),
  VOCABULARY(VocabularyRoute, Res.string.navigation_tab_vocabulary, Res.drawable.ic_tab_vocabulary),
  PROGRESS(ProgressRoute, Res.string.navigation_tab_progress, Res.drawable.ic_tab_progress),
  PROFILE(ProfileRoute, Res.string.navigation_tab_profile, Res.drawable.ic_tab_profile),
}
