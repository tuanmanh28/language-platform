package com.app.platform.language.ui.comingsoon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.platform.language.ui.components.LpEmptyState
import com.app.platform.language.ui.components.LpPreviews
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_coming_soon
import com.app.platform.language.ui.resources.common_coming_soon_message
import com.app.platform.language.ui.resources.navigation_tab_vocabulary
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComingSoonScreen(title: StringResource) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { TopAppBar(title = { Text(stringResource(title)) }) },
  ) { padding ->
    LpEmptyState(
      title = stringResource(Res.string.common_coming_soon),
      message = stringResource(Res.string.common_coming_soon_message),
      modifier = Modifier.fillMaxSize().padding(padding),
    )
  }
}

@LpPreviews
@Composable
private fun ComingSoonPreview() {
  LanguagePlatformTheme {
    ComingSoonScreen(title = Res.string.navigation_tab_vocabulary)
  }
}
