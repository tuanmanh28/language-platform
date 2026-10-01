package com.app.platform.language.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.platform.language.ui.components.LpPreviews
import com.app.platform.language.ui.components.LpTestCard
import com.app.platform.language.ui.components.TestCardState
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_coming_soon
import com.app.platform.language.ui.resources.practice_home_section_title
import com.app.platform.language.ui.resources.practice_home_skill_available
import com.app.platform.language.ui.resources.practice_home_skill_listening
import com.app.platform.language.ui.resources.practice_home_skill_reading
import com.app.platform.language.ui.resources.practice_home_skill_speaking
import com.app.platform.language.ui.resources.practice_home_skill_writing
import com.app.platform.language.ui.resources.practice_home_title
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

private val ComingSoonSkills =
  listOf(
    Res.string.practice_home_skill_listening,
    Res.string.practice_home_skill_writing,
    Res.string.practice_home_skill_speaking,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PracticeHomeScreen(onOpenReading: () -> Unit) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { TopAppBar(title = { Text(stringResource(Res.string.practice_home_title)) }) },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(LanguagePlatformTheme.spacing.lg),
      verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.md),
    ) {
      item(key = "section-title") {
        Text(stringResource(Res.string.practice_home_section_title), style = MaterialTheme.typography.titleSmall)
      }

      item(key = "reading") {
        LpTestCard(
          title = stringResource(Res.string.practice_home_skill_reading),
          details = stringResource(Res.string.practice_home_skill_available),
          onClick = onOpenReading,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      items(ComingSoonSkills, key = { it.key }) { skill ->
        LpTestCard(
          title = stringResource(skill),
          details = stringResource(Res.string.common_coming_soon),
          onClick = {},
          modifier = Modifier.fillMaxWidth(),
          state = TestCardState.DISABLED,
        )
      }
    }
  }
}

@LpPreviews
@Composable
private fun PracticeHomePreview() {
  LanguagePlatformTheme {
    PracticeHomeScreen(onOpenReading = {})
  }
}
