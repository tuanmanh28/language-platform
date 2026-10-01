package com.app.platform.language.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.platform.language.core.model.QuestionResult
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.ui.components.BandBadgeSize
import com.app.platform.language.ui.components.LpBandBadge
import com.app.platform.language.ui.components.LpPreviews
import com.app.platform.language.ui.components.LpPrimaryButton
import com.app.platform.language.ui.components.LpSecondaryButton
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_answer_correct
import com.app.platform.language.ui.resources.common_answer_wrong
import com.app.platform.language.ui.resources.common_mark_correct
import com.app.platform.language.ui.resources.common_mark_wrong
import com.app.platform.language.ui.resources.reading_result_accepted_answers
import com.app.platform.language.ui.resources.reading_result_back_to_list
import com.app.platform.language.ui.resources.reading_result_correct_count
import com.app.platform.language.ui.resources.reading_result_details
import com.app.platform.language.ui.resources.reading_result_estimated_band
import com.app.platform.language.ui.resources.reading_result_no_answer
import com.app.platform.language.ui.resources.reading_result_question_number
import com.app.platform.language.ui.resources.reading_result_restart
import com.app.platform.language.ui.resources.reading_result_time_expired
import com.app.platform.language.ui.resources.reading_result_title
import com.app.platform.language.ui.resources.reading_result_your_answer
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource

private const val ACCEPTED_ANSWERS_SEPARATOR = " / "

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingResultContent(
  state: ReadingSessionUiState.Finished,
  onRestart: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text(stringResource(Res.string.reading_result_title)) }) },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(LanguagePlatformTheme.spacing.lg),
      verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm),
    ) {
      item(key = "summary") {
        ResultSummary(state, onRestart, onExit, Modifier.fillMaxWidth())
      }

      item(key = "details-title") {
        Text(
          stringResource(Res.string.reading_result_details),
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.padding(top = LanguagePlatformTheme.spacing.sm),
        )
      }

      items(state.result.questionResults, key = { it.questionId }) { item ->
        QuestionResultRow(item, Modifier.fillMaxWidth())
      }
    }
  }
}

@Composable
private fun ResultSummary(
  state: ReadingSessionUiState.Finished,
  onRestart: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val result = state.result

  Card(modifier) {
    Column(
      Modifier.fillMaxWidth().padding(LanguagePlatformTheme.spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(stringResource(Res.string.reading_result_estimated_band), style = MaterialTheme.typography.labelLarge)

      Spacer(Modifier.height(LanguagePlatformTheme.spacing.sm))

      LpBandBadge(result.band, size = BandBadgeSize.LARGE)

      Spacer(Modifier.height(LanguagePlatformTheme.spacing.sm))

      Text(
        stringResource(Res.string.reading_result_correct_count, result.correctCount, result.totalQuestions),
        style = MaterialTheme.typography.titleMedium,
      )

      if (state.isTimeExpired) {
        Spacer(Modifier.height(LanguagePlatformTheme.spacing.sm))

        Text(stringResource(Res.string.reading_result_time_expired), color = MaterialTheme.colorScheme.error)
      }

      Spacer(Modifier.height(LanguagePlatformTheme.spacing.lg))

      Row(horizontalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.md)) {
        LpSecondaryButton(text = stringResource(Res.string.reading_result_back_to_list), onClick = onExit)

        LpPrimaryButton(text = stringResource(Res.string.reading_result_restart), onClick = onRestart)
      }
    }
  }
}

@Composable
private fun QuestionResultRow(
  item: QuestionResult,
  modifier: Modifier = Modifier,
) {
  val verdict =
    stringResource(if (item.isCorrect) Res.string.common_answer_correct else Res.string.common_answer_wrong)

  Row(modifier.padding(vertical = LanguagePlatformTheme.spacing.xs), verticalAlignment = Alignment.Top) {
    Text(
      text = stringResource(if (item.isCorrect) Res.string.common_mark_correct else Res.string.common_mark_wrong),
      color = if (item.isCorrect) LanguagePlatformTheme.colors.correct else LanguagePlatformTheme.colors.wrong,
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.semantics { contentDescription = verdict },
    )

    Spacer(Modifier.width(LanguagePlatformTheme.spacing.md))

    Column {
      Text(
        stringResource(Res.string.reading_result_question_number, item.number),
        style = MaterialTheme.typography.titleSmall,
      )

      Text(
        stringResource(
          Res.string.reading_result_your_answer,
          item.userAnswer ?: stringResource(Res.string.reading_result_no_answer),
        ),
        style = MaterialTheme.typography.bodyMedium,
      )

      if (!item.isCorrect) {
        Text(
          stringResource(
            Res.string.reading_result_accepted_answers,
            item.acceptedAnswers.joinToString(ACCEPTED_ANSWERS_SEPARATOR),
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@LpPreviews
@Composable
private fun ReadingResultContentPreview() {
  LanguagePlatformTheme {
    ReadingResultContent(ReadingPreviewData.sessionFinished, onRestart = {}, onExit = {})
  }
}
