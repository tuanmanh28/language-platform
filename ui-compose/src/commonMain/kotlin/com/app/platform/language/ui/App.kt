package com.app.platform.language.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.app.platform.language.ui.reading.ReadingSessionScreen
import com.app.platform.language.ui.reading.ReadingTestListScreen
import com.app.platform.language.ui.theme.LanguagePlatformTheme

private sealed interface Screen {
  data object TestList : Screen

  data class Session(
    val testId: String,
  ) : Screen
}

/** Root composable for Android and Desktop. Koin must be started first (initKoin). */
@Composable
fun LanguagePlatformApp() {
  LanguagePlatformTheme {
    var screen by remember { mutableStateOf<Screen>(Screen.TestList) }

    when (val current = screen) {
      Screen.TestList -> {
        ReadingTestListScreen(
          onOpenTest = { testId -> screen = Screen.Session(testId) },
        )
      }

      is Screen.Session -> {
        ScopedViewModels(key = current) {
          ReadingSessionScreen(
            testId = current.testId,
            onExit = { screen = Screen.TestList },
          )
        }
      }
    }
  }
}

/**
 * Gives each opened screen its own ViewModelStore, cleared when the screen is left.
 * Every visit to a test is a fresh attempt (the timer cannot be "paused" by leaving and coming back).
 * Once the app has more screens, replace this with navigation-compose (each back-stack entry owns a store).
 */
@Composable
private fun ScopedViewModels(
  key: Any,
  content: @Composable () -> Unit,
) {
  val owner =
    remember(key) {
      object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
      }
    }
  DisposableEffect(owner) {
    onDispose { owner.viewModelStore.clear() }
  }
  CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
    content()
  }
}

/** System Back button (Android). Desktop has none, so it is a no-op there. */
@Composable
internal expect fun PlatformBackHandler(
  enabled: Boolean = true,
  onBack: () -> Unit,
)
