import Shared
import SwiftUI

struct ReadingResultView: View {
  let finished: ReadingSessionUiState.Finished
  let onRestart: () -> Void
  @Environment(\.dismiss) private var dismiss

  var body: some View {
    List {
      Section {
        ResultSummary(finished: finished, onBackToList: { dismiss() }, onRestart: onRestart)
      }
      Section("reading_result_details") {
        ForEach(finished.result.questionResults, id: \.questionId) { item in
          QuestionResultRow(item: item)
        }
      }
    }
    .navigationTitle("reading_result_title")
  }
}

private struct ResultSummary: View {
  let finished: ReadingSessionUiState.Finished
  let onBackToList: () -> Void
  let onRestart: () -> Void

  var body: some View {
    let result = finished.result
    VStack(spacing: 8) {
      Text("reading_result_estimated_band")
        .font(.subheadline)
        .foregroundStyle(.secondary)
      Text(String(format: "%.1f", result.band))
        .font(.system(size: 56, weight: .bold, design: .rounded))
      Text(
        String(format: String(localized: "reading_result_correct_count"), result.correctCount, result.totalQuestions)
      )
      .font(.headline)
      if finished.isTimeExpired {
        Text("reading_result_time_expired")
          .foregroundStyle(.red)
      }
      HStack(spacing: 12) {
        Button("reading_result_back_to_list", action: onBackToList)
          .buttonStyle(.bordered)
        Button("reading_result_restart", action: onRestart)
          .buttonStyle(.borderedProminent)
      }
      .padding(.top, 8)
    }
    .frame(maxWidth: .infinity)
    .padding(.vertical, 12)
  }
}

private struct QuestionResultRow: View {
  let item: QuestionResult

  var body: some View {
    HStack(alignment: .top, spacing: 12) {
      Image(systemName: item.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
        .foregroundStyle(item.isCorrect ? Color.green : Color.red)
        .accessibilityLabel(item.isCorrect ? Text("reading_result_correct") : Text("reading_result_wrong"))
      VStack(alignment: .leading, spacing: 2) {
        Text(String(format: String(localized: "reading_result_question_number"), item.number))
          .font(.subheadline.bold())
        Text(String(format: String(localized: "reading_result_your_answer"), userAnswer))
          .font(.callout)
        if !item.isCorrect {
          Text(String(format: String(localized: "reading_result_accepted_answers"), acceptedAnswers))
            .font(.callout)
            .foregroundStyle(.secondary)
        }
      }
    }
  }

  private var userAnswer: String { item.userAnswer ?? String(localized: "reading_result_no_answer") }

  private var acceptedAnswers: String { item.acceptedAnswers.joined(separator: " / ") }
}

#Preview {
  NavigationStack {
    ReadingResultView(finished: ReadingPreviewData.sessionFinished, onRestart: {})
  }
}
