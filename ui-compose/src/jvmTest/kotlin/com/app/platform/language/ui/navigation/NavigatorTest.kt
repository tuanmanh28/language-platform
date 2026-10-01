package com.app.platform.language.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
  private val topLevelRoutes = TopLevelTab.entries.map { it.route }
  private val state =
    NavState(
      startRoute = PracticeRoute,
      topLevelRoute = mutableStateOf(PracticeRoute),
      backStacks = topLevelRoutes.associateWith { mutableStateListOf(it) },
    )
  private val navigator = Navigator(state)

  @Test
  fun startsOnPracticeTabRoot() {
    assertEquals(PracticeRoute, state.topLevelRoute)
    assertEquals(listOf<AppRoute>(PracticeRoute), state.currentBackStack)
    assertEquals(listOf<TopLevelRoute>(PracticeRoute), state.stacksInUse)
  }

  @Test
  fun selectingTabShowsItAboveStartTab() {
    navigator.selectTab(VocabularyRoute)

    assertEquals(VocabularyRoute, state.topLevelRoute)
    assertEquals(listOf<AppRoute>(VocabularyRoute), state.currentBackStack)
    assertEquals(listOf(PracticeRoute, VocabularyRoute), state.stacksInUse)
  }

  @Test
  fun openingSessionFromListPushesBothRoutes() {
    navigator.add(ReadingTestListRoute)
    navigator.add(ReadingSessionRoute("test-1"))

    assertEquals(listOf(PracticeRoute, ReadingTestListRoute, ReadingSessionRoute("test-1")), state.currentBackStack)
  }

  @Test
  fun backFromSessionReturnsToList() {
    navigator.add(ReadingTestListRoute)
    navigator.add(ReadingSessionRoute("test-1"))

    navigator.goBack()

    assertEquals(listOf(PracticeRoute, ReadingTestListRoute), state.currentBackStack)
  }

  @Test
  fun switchingTabsKeepsEachTabsBackStack() {
    navigator.add(ReadingTestListRoute)
    navigator.add(ReadingSessionRoute("test-1"))

    navigator.selectTab(ProgressRoute)
    navigator.selectTab(PracticeRoute)

    assertEquals(listOf(PracticeRoute, ReadingTestListRoute, ReadingSessionRoute("test-1")), state.currentBackStack)
  }

  @Test
  fun reselectingCurrentTabPopsToItsRoot() {
    navigator.add(ReadingTestListRoute)
    navigator.add(ReadingSessionRoute("test-1"))

    navigator.selectTab(PracticeRoute)

    assertEquals(listOf<AppRoute>(PracticeRoute), state.currentBackStack)
  }

  @Test
  fun addingTopLevelRouteSelectsItsTab() {
    navigator.add(ProfileRoute)

    assertEquals(ProfileRoute, state.topLevelRoute)
    assertEquals(listOf<AppRoute>(PracticeRoute), state.backStacks.getValue(PracticeRoute))
  }

  @Test
  fun backFromOtherTabRootReturnsToStartTab() {
    navigator.add(ReadingTestListRoute)
    navigator.selectTab(VocabularyRoute)

    navigator.goBack()

    assertEquals(PracticeRoute, state.topLevelRoute)
    assertEquals(listOf(PracticeRoute, ReadingTestListRoute), state.currentBackStack)
  }

  @Test
  fun backOnStartTabRootKeepsStack() {
    navigator.goBack()

    assertEquals(PracticeRoute, state.topLevelRoute)
    assertEquals(listOf<AppRoute>(PracticeRoute), state.currentBackStack)
  }
}
