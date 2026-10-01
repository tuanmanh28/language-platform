package com.app.platform.language.ui.navigation

internal class Navigator(
  private val state: NavState,
) {
  fun add(route: AppRoute) {
    when (route) {
      is TopLevelRoute -> selectTab(route)
      else -> state.currentBackStack.add(route)
    }
  }

  fun selectTab(route: TopLevelRoute) {
    if (route == state.topLevelRoute) {
      popToRoot()
    } else {
      state.topLevelRoute = route
    }
  }

  fun goBack() {
    val backStack = state.currentBackStack
    when {
      backStack.size > 1 -> backStack.removeAt(backStack.lastIndex)
      state.topLevelRoute != state.startRoute -> state.topLevelRoute = state.startRoute
    }
  }

  private fun popToRoot() {
    val backStack = state.currentBackStack
    backStack.removeRange(1, backStack.size)
  }
}
