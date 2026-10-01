package com.app.platform.language.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import com.app.platform.language.core.model.ReadingError
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.ui.PlatformBackHandler
import com.app.platform.language.ui.resources.Res
import com.app.platform.language.ui.resources.common_retry
import com.app.platform.language.ui.resources.reading_session_answer_placeholder
import com.app.platform.language.ui.resources.reading_session_continue
import com.app.platform.language.ui.resources.reading_session_exit
import com.app.platform.language.ui.resources.reading_session_max_words
import com.app.platform.language.ui.resources.reading_session_option
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
private const val TIME_RUNNING_OUT_SECONDS = 60

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
      Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
    }

    is ReadingSessionUiState.Failed -> {
      FailedContent(state.error, onRetry, Modifier.fillMaxSize())
    }

    is ReadingSessionUiState.InProgress -> {
      InProgressContent(state, onAnswer, onSubmit, onExit, Modifier.fillMaxSize())
    }

    is ReadingSessionUiState.Finished -> {
      ReadingResultContent(state, onRestart, onExit, Modifier.fillMaxSize())
    }
  }
}

@Composable
private fun FailedContent(
  error: ReadingError,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(error.toUserMessage(), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onRetry) { Text(stringResource(Res.string.common_retry)) }
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
  val timerColor =
    if (state.remainingSeconds <= TIME_RUNNING_OUT_SECONDS) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.onSurface
    }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(state.test.title, maxLines = 1) },
        navigationIcon = {
          TextButton(onClick = onExit) { Text(stringResource(Res.string.reading_session_exit)) }
        },
        actions = {
          Text(text = state.remainingLabel, style = MaterialTheme.typography.titleMedium, color = timerColor)
          Spacer(Modifier.width(12.dp))
          Button(onClick = { isConfirmingSubmit = true }) { Text(stringResource(Res.string.reading_session_submit)) }
          Spacer(Modifier.width(8.dp))
        },
      )
    },
  ) { padding ->
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
      if (maxWidth >= TwoPaneMinWidth) {
        Row(Modifier.fillMaxSize()) {
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(24.dp),
          ) { passages(state.test.passages) }
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) { questions(state, onAnswer) }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
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
      Button(onClick = onConfirm) { Text(stringResource(Res.string.reading_session_submit)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.reading_session_continue)) }
    },
  )
}

private fun LazyListScope.passages(passages: List<Passage>) {
  passages.forEach { passage ->
    item(key = "passage-${passage.id}") {
      Text(
        text = passage.title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp),
      )
    }
    items(passage.paragraphs, key = { "paragraph-${passage.id}-${it.label}-${it.text.hashCode()}" }) { paragraph ->
      ParagraphText(paragraph, Modifier.padding(bottom = 12.dp))
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
  onAnswer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(
      stringResource(Res.string.reading_session_question, question.number, question.prompt),
      style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(8.dp))

    when (group.type) {
      QuestionType.TRUE_FALSE_NOT_GIVEN,
      QuestionType.YES_NO_NOT_GIVEN,
      -> FixedChoiceAnswer(group.type.fixedChoices, answer, onAnswer)

      QuestionType.MULTIPLE_CHOICE -> MultipleChoiceAnswer(question, answer, onAnswer, Modifier.fillMaxWidth())

      QuestionType.SENTENCE_COMPLETION -> CompletionAnswer(group.maxWords, answer, onAnswer, Modifier.fillMaxWidth())
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
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    choices.forEach { choice ->
      FilterChip(
        selected = answer.equals(choice, ignoreCase = true),
        onClick = { onAnswer(choice) },
        label = { Text(choice) },
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
  Column(modifier) {
    question.options.forEach { option ->
      val isSelected = answer == option.key
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = { onAnswer(option.key) }, role = Role.RadioButton),
      ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
          stringResource(Res.string.reading_session_option, option.key, option.text),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun CompletionAnswer(
  maxWords: Int?,
  answer: String,
  onAnswer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = answer,
    onValueChange = onAnswer,
    singleLine = true,
    placeholder = {
      Text(
        if (maxWords != null) {
          stringResource(Res.string.reading_session_max_words, maxWords)
        } else {
          stringResource(Res.string.reading_session_answer_placeholder)
        },
      )
    },
    modifier = modifier,
  )
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
