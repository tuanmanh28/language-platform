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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.platform.language.core.model.QuestionResult
import com.app.platform.language.shared.reading.ReadingSessionUiState
import com.app.platform.language.ui.theme.ResultColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingResultScreen(
  state: ReadingSessionUiState.Finished,
  onRestart: () -> Unit,
  onExit: () -> Unit,
) {
  val result = state.result

  Scaffold(topBar = { TopAppBar(title = { Text("Kết quả") }) }) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item(key = "summary") {
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Band ước tính", style = MaterialTheme.typography.labelLarge)
            Text(result.band.toString(), style = MaterialTheme.typography.displayLarge)
            Text(
              "Đúng ${result.correctCount}/${result.totalQuestions} câu",
              style = MaterialTheme.typography.titleMedium,
            )
            if (state.timeExpired) {
              Spacer(Modifier.height(8.dp))
              Text("Hết giờ — bài đã được nộp tự động.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(onClick = onExit) { Text("Về danh sách") }
              Button(onClick = onRestart) { Text("Làm lại") }
            }
          }
        }
      }
      items(result.questionResults, key = { it.questionId }) { item ->
        QuestionResultRow(item)
      }
    }
  }
}

@Composable
private fun QuestionResultRow(item: QuestionResult) {
  Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
    Text(
      text = if (item.isCorrect) "✓" else "✗",
      color = if (item.isCorrect) ResultColors.correct else ResultColors.wrong,
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.width(12.dp))
    Column {
      Text("Câu ${item.number}", style = MaterialTheme.typography.titleSmall)
      Text(
        "Bạn trả lời: ${item.userAnswer ?: "—"}",
        style = MaterialTheme.typography.bodyMedium,
      )
      if (!item.isCorrect) {
        Text(
          "Đáp án: ${item.acceptedAnswers.joinToString(" / ")}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
