package com.app.platform.language.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppRoute

@Serializable
internal sealed interface TopLevelRoute : AppRoute

@Serializable
@SerialName("Practice")
internal data object PracticeRoute : TopLevelRoute

@Serializable
@SerialName("Vocabulary")
internal data object VocabularyRoute : TopLevelRoute

@Serializable
@SerialName("Progress")
internal data object ProgressRoute : TopLevelRoute

@Serializable
@SerialName("Profile")
internal data object ProfileRoute : TopLevelRoute

@Serializable
@SerialName("ReadingTestList")
internal data object ReadingTestListRoute : AppRoute

@Serializable
@SerialName("ReadingSession")
internal data class ReadingSessionRoute(
  val testId: String,
) : AppRoute
