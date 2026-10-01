package com.app.platform.language.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.platform.language.core.model.Paragraph
import com.app.platform.language.core.model.Passage
import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionType
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.ui.PlatformBackHandler
import com.app.platform.language.ui.components.AnswerState
import com.app.platform.language.ui.components.LpAnswerChip
import com.app.platform.language.ui.components.LpErrorState
import com.app.platform.language.ui.components.LpGapField
import com.app.platform.language.ui.components.LpOptionRow
import com.app.platform.language.ui.components.LpPrimaryButton
import com.app.platform.language.ui.components.LpProgressRing
import com.app.platform.language.ui.components.LpTextButton
import com.app.platform.language.ui.components.LpTimerBar
import com.app.platform.language.ui.components.TimerBarState
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.reading_session_continue
import com.app.platform.language.ui.resources.reading_session_exit
import com.app.platform.language.ui.resources.reading_session_question
import com.app.platform.language.ui.resources.reading_session_submit
import com.app.platform.language.ui.resources.reading_session_submit_all_answered
import com.app.platform.language.ui.resources.reading_session_submit_title
import com.app.platform.language.ui.resources.reading_session_submit_unanswered
import com.app.platform.language.ui.theme.LanguagePlatformTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val TwoPaneMinWidth = 840.dp

@Composable
internal fun ReadingSessionScreen(
  testId: String,
  onExit: () -> Unit,
  viewModel: ReadingSessionViewModel = koinViewModel(key = "reading-session-$testId") { parametersOf(testId) },
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  DisposableEffect(viewModel) {
    viewModel.start()
    onDispose { viewModel.stop() }
  }

  PlatformBackHandler(onBack = onExit)

  ReadingSessionScreen(
    state = state,
    onRetry = viewModel::retry,
    onAnswer = viewModel::answer,
    onSubmit = viewModel::submit,
    onRestart = viewModel::restart,
    onExit = onExit,
  )
}

@Composable
internal fun ReadingSessionScreen(
  state: ReadingSessionUiState,
  onRetry: () -> Unit,
  onAnswer: (questionId: String, value: String) -> Unit,
  onSubmit: () -> Unit,
  onRestart: () -> Unit,
  onExit: () -> Unit,
) {
  when (state) {
    ReadingSessionUiState.Loading -> {
      Box(Modifier.fillMaxSize()) { LpProgressRing(progress = null, modifier = Modifier.align(Alignment.Center)) }
    }

    is ReadingSessionUiState.Failed -> {
      LpErrorState(state.error.toUserMessage(), onRetry, Modifier.fillMaxSize())
    }

    is ReadingSessionUiState.InProgress -> {
      InProgressContent(state, onAnswer, onSubmit, onExit, Modifier.fillMaxSize())
    }

    is ReadingSessionUiState.Finished -> {
      ReadingResultContent(state, onRestart, onExit, Modifier.fillMaxSize())
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InProgressContent(
  state: ReadingSessionUiState.InProgress,
  onAnswer: (questionId: String, value: String) -> Unit,
  onSubmit: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isConfirmingSubmit by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      Column {
        TopAppBar(
          title = { Text(state.test.title, maxLines = 1) },
          navigationIcon = {
            LpTextButton(text = stringResource(Res.string.reading_session_exit), onClick = onExit)
          },
          actions = {
            LpPrimaryButton(
              text = stringResource(Res.string.reading_session_submit),
              onClick = { isConfirmingSubmit = true },
            )

            Spacer(Modifier.width(LanguagePlatformTheme.spacing.sm))
          },
        )

        LpTimerBar(
          remainingLabel = state.remainingLabel,
          progress = state.remainingFraction,
          state = if (state.isTimeRunningOut) TimerBarState.WARNING else TimerBarState.NORMAL,
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = LanguagePlatformTheme.spacing.lg)
              .padding(bottom = LanguagePlatformTheme.spacing.sm),
        )
      }
    },
  ) { padding ->
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
      if (maxWidth >= TwoPaneMinWidth) {
        Row(Modifier.fillMaxSize()) {
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(LanguagePlatformTheme.spacing.xl),
          ) { passages(state.test.passages) }

          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(LanguagePlatformTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.lg),
          ) { questions(state, onAnswer) }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(LanguagePlatformTheme.spacing.lg),
          verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.lg),
        ) {
          passages(state.test.passages)

          item(key = "divider") { HorizontalDivider() }

          questions(state, onAnswer)
        }
      }
    }
  }

  if (isConfirmingSubmit) {
    SubmitDialog(
      state = state,
      onConfirm = {
        isConfirmingSubmit = false
        onSubmit()
      },
      onDismiss = { isConfirmingSubmit = false },
    )
  }
}

@Composable
private fun SubmitDialog(
  state: ReadingSessionUiState.InProgress,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val total = state.test.questionCount
  val unanswered = total - state.answeredCount
  val message =
    if (unanswered > 0) {
      stringResource(Res.string.reading_session_submit_unanswered, unanswered, total)
    } else {
      stringResource(Res.string.reading_session_submit_all_answered, total)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.reading_session_submit_title)) },
    text = { Text(message) },
    confirmButton = {
      LpPrimaryButton(text = stringResource(Res.string.reading_session_submit), onClick = onConfirm)
    },
    dismissButton = {
      LpTextButton(text = stringResource(Res.string.reading_session_continue), onClick = onDismiss)
    },
  )
}

private fun LazyListScope.passages(passages: List<Passage>) {
  passages.forEach { passage ->
    item(key = "passage-${passage.id}") {
      Text(
        text = passage.title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = LanguagePlatformTheme.spacing.md),
      )
    }

    items(passage.paragraphs, key = { "paragraph-${passage.id}-${it.label}-${it.text.hashCode()}" }) { paragraph ->
      ParagraphText(paragraph, Modifier.padding(bottom = LanguagePlatformTheme.spacing.md))
    }
  }
}

@Composable
private fun ParagraphText(
  paragraph: Paragraph,
  modifier: Modifier = Modifier,
) {
  Text(
    text =
      buildAnnotatedString {
        paragraph.label?.let { label ->
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(label) }
          append("  ")
        }
        append(paragraph.text)
      },
    style = MaterialTheme.typography.bodyLarge,
    modifier = modifier,
  )
}

private fun LazyListScope.questions(
  state: ReadingSessionUiState.InProgress,
  onAnswer: (questionId: String, value: String) -> Unit,
) {
  state.test.passages.flatMap { it.questionGroups }.forEach { group ->
    item(key = "group-${group.id}") {
      Text(
        text = group.instruction,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    items(group.questions, key = { "question-${it.id}" }) { question ->
      QuestionItem(
        group = group,
        question = question,
        answer = state.answerFor(question.id),
        wordCount = state.wordCountFor(question.id),
        onAnswer = { onAnswer(question.id, it) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun QuestionItem(
  group: QuestionGroup,
  question: Question,
  answer: String,
  wordCount: Int,
  onAnswer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(
      stringResource(Res.string.reading_session_question, question.number, question.prompt),
      style = MaterialTheme.typography.bodyLarge,
    )

    Spacer(Modifier.height(LanguagePlatformTheme.spacing.sm))

    when (group.type) {
      QuestionType.TRUE_FALSE_NOT_GIVEN,
      QuestionType.YES_NO_NOT_GIVEN,
      -> {
        FixedChoiceAnswer(group.type.fixedChoices, answer, onAnswer)
      }

      QuestionType.MULTIPLE_CHOICE -> {
        MultipleChoiceAnswer(question, answer, onAnswer, Modifier.fillMaxWidth())
      }

      QuestionType.SENTENCE_COMPLETION -> {
        LpGapField(answer, wordCount, group.maxWords, onAnswer, Modifier.fillMaxWidth())
      }
    }
  }
}

@Composable
private fun FixedChoiceAnswer(
  choices: List<String>,
  answer: String,
  onAnswer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm),
    verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm),
  ) {
    choices.forEach { choice ->
      LpAnswerChip(
        text = choice,
        state = if (answer.equals(choice, ignoreCase = true)) AnswerState.SELECTED else AnswerState.IDLE,
        onClick = { onAnswer(choice) },
      )
    }
  }
}

@Composable
private fun MultipleChoiceAnswer(
  question: Question,
  answer: String,
  onAnswer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(LanguagePlatformTheme.spacing.sm)) {
    question.options.forEach { option ->
      LpOptionRow(
        key = option.key,
        text = option.text,
        state = if (answer == option.key) AnswerState.SELECTED else AnswerState.IDLE,
        onClick = { onAnswer(option.key) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Preview
@Composable
private fun ReadingSessionLoadingPreview() {
  LanguagePlatformTheme {
    ReadingSessionScreen(ReadingSessionUiState.Loading, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionLoadingDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingSessionScreen(ReadingSessionUiState.Loading, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionFailedPreview() {
  LanguagePlatformTheme {
    ReadingSessionScreen(ReadingPreviewData.sessionFailed, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionFailedDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingSessionScreen(ReadingPreviewData.sessionFailed, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionInProgressPreview() {
  LanguagePlatformTheme {
    ReadingSessionScreen(ReadingPreviewData.sessionInProgress, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionInProgressDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingSessionScreen(ReadingPreviewData.sessionInProgress, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionFinishedPreview() {
  LanguagePlatformTheme {
    ReadingSessionScreen(ReadingPreviewData.sessionFinished, {}, { _, _ -> }, {}, {}, {})
  }
}

@Preview
@Composable
private fun ReadingSessionFinishedDarkPreview() {
  LanguagePlatformTheme(darkTheme = true) {
    ReadingSessionScreen(ReadingPreviewData.sessionFinished, {}, { _, _ -> }, {}, {}, {})
  }
}
