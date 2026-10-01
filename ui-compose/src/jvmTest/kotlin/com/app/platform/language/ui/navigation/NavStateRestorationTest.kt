package com.app.platform.language.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.test.Test
import kotlin.test.assertEquals

class NavStateRestorationTest {
  @Test
  fun backStackSurvivesSaveAndRestore() {
    val serializer = SnapshotStateListSerializer(AppRoute.serializer())
    val backStack = mutableStateListOf(PracticeRoute, ReadingTestListRoute, ReadingSessionRoute("test-1"))

    val restored = decodeFromSavedState(serializer, encodeToSavedState(serializer, backStack))

    assertEquals(backStack.toList(), restored.toList())
  }

  @Test
  fun selectedTabSurvivesSaveAndRestore() {
    val serializer = MutableStateSerializer(TopLevelRoute.serializer())
    val selectedTab = mutableStateOf<TopLevelRoute>(ProgressRoute)

    val restored = decodeFromSavedState(serializer, encodeToSavedState(serializer, selectedTab))

    assertEquals(ProgressRoute, restored.value)
  }
}
