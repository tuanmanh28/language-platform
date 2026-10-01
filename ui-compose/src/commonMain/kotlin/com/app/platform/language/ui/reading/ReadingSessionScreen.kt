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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.platform.language.core.model.Passage
import com.app.platform.language.core.model.Question
import com.app.platform.language.core.model.QuestionGroup
import com.app.platform.language.core.model.QuestionType
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.ui.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Screens wider than this (tablet, desktop) show the passage and questions side by side. */
private val TwoPaneMinWidth = 840.dp

@Composable
fun ReadingSessionScreen(testId: String, onExit: () -> Unit) {
    val viewModel = koinViewModel<ReadingSessionViewModel>(key = "reading-session-$testId") {
        parametersOf(testId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }
    PlatformBackHandler(onBack = onExit)

    when (val current = state) {
        ReadingSessionUiState.Loading -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        is ReadingSessionUiState.Error -> Box(Modifier.fillMaxSize()) {
            ErrorState(current.message, onRetry = viewModel::retry, modifier = Modifier.align(Alignment.Center))
        }

        is ReadingSessionUiState.InProgress -> InProgressContent(
            state = current,
            onAnswer = viewModel::answer,
            onSubmit = viewModel::submit,
            onExit = onExit,
        )

        is ReadingSessionUiState.Finished -> ReadingResultScreen(
            state = current,
            onRestart = viewModel::restart,
            onExit = onExit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InProgressContent(
    state: ReadingSessionUiState.InProgress,
    onAnswer: (questionId: String, value: String) -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
) {
    var confirmSubmit by remember { mutableStateOf(false) }
    val total = state.test.questionCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.test.title, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onExit) { Text("Thoát") } },
                actions = {
                    Text(
                        text = state.remainingLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.remainingSeconds <= 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { confirmSubmit = true }) { Text("Nộp bài") }
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

    if (confirmSubmit) {
        val unanswered = total - state.answeredCount
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Nộp bài?") },
            text = {
                Text(
                    if (unanswered > 0) "Bạn còn $unanswered/$total câu chưa trả lời." else "Bạn đã trả lời đủ $total câu.",
                )
            },
            confirmButton = {
                Button(onClick = { confirmSubmit = false; onSubmit() }) { Text("Nộp bài") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSubmit = false }) { Text("Làm tiếp") }
            },
        )
    }
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
            Text(
                text = buildAnnotatedString {
                    paragraph.label?.let { label ->
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$label  ") }
                    }
                    append(paragraph.text)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

private fun LazyListScope.questions(
    state: ReadingSessionUiState.InProgress,
    onAnswer: (String, String) -> Unit,
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
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionItem(
    group: QuestionGroup,
    question: Question,
    answer: String,
    onAnswer: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("${question.number}. ${question.prompt}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        when (group.type) {
            QuestionType.TRUE_FALSE_NOT_GIVEN,
            QuestionType.YES_NO_NOT_GIVEN -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.type.fixedChoices.forEach { choice ->
                    FilterChip(
                        selected = answer.equals(choice, ignoreCase = true),
                        onClick = { onAnswer(choice) },
                        label = { Text(choice) },
                    )
                }
            }

            QuestionType.MULTIPLE_CHOICE -> Column {
                question.options.forEach { option ->
                    val selected = answer == option.key
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, onClick = { onAnswer(option.key) }, role = Role.RadioButton),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text("${option.key}. ${option.text}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            QuestionType.SENTENCE_COMPLETION -> OutlinedTextField(
                value = answer,
                onValueChange = onAnswer,
                singleLine = true,
                placeholder = {
                    Text(group.maxWords?.let { "Tối đa $it từ" } ?: "Câu trả lời")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
