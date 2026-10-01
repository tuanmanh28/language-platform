package com.app.platform.language.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer

internal class NavState(
  val startRoute: TopLevelRoute,
  topLevelRoute: MutableState<TopLevelRoute>,
  val backStacks: Map<TopLevelRoute, SnapshotStateList<AppRoute>>,
) {
  var topLevelRoute: TopLevelRoute by topLevelRoute

  val currentBackStack: SnapshotStateList<AppRoute> get() = backStacks.getValue(topLevelRoute)

  // The start tab stays below every other tab, so Back from another tab's root returns to it.
  val stacksInUse: List<TopLevelRoute> get() = listOf(startRoute, topLevelRoute).distinct()
}

@Composable
internal fun rememberNavState(
  startRoute: TopLevelRoute,
  topLevelRoutes: List<TopLevelRoute>,
): NavState {
  val topLevelRoute =
    rememberSerializable(serializer = MutableStateSerializer(TopLevelRoute.serializer())) {
      mutableStateOf(startRoute)
    }
  val backStacks =
    topLevelRoutes.associateWith { route ->
      rememberSerializable(serializer = SnapshotStateListSerializer(AppRoute.serializer())) {
        mutableStateListOf<AppRoute>(route)
      }
    }

  return remember(startRoute, topLevelRoutes) { NavState(startRoute, topLevelRoute, backStacks) }
}
