import Shared
import SwiftUI

struct ReadingSessionView: View {
  let testId: String
  @State private var owner: ViewModelOwner<ReadingSessionViewModel>?
  @State private var state: ReadingSessionUiState = ReadingSessionUiState.Loading.shared

  var body: some View {
    ReadingSessionContent(
      state: state,
      onRetry: { owner?.viewModel.retry() },
      onAnswer: { questionId, value in owner?.viewModel.answer(questionId: questionId, value: value) },
      onSubmit: { owner?.viewModel.submit() },
      onRestart: { owner?.viewModel.restart() }
    )
    .task {
      let owner = ViewModels.shared.readingSession(testId: testId)
      self.owner = owner
      owner.viewModel.start()
      await withTaskCancellationHandler {
        for await value in owner.viewModel.state { state = value }
      } onCancel: {
        owner.clear()
      }
    }
  }
}

struct ReadingSessionContent: View {
  let state: ReadingSessionUiState
  let onRetry: () -> Void
  let onAnswer: (String, String) -> Void
  let onSubmit: () -> Void
  let onRestart: () -> Void

  var body: some View {
    switch onEnum(of: state) {
    case .loading:
      ProgressView()
        .frame(maxWidth: .infinity, maxHeight: .infinity)

    case .failed(let failed):
      ReadingErrorView(message: failed.error.userMessage, onRetry: onRetry)

    case .inProgress(let session):
      InProgressView(session: session, onAnswer: onAnswer, onSubmit: onSubmit)

    case .finished(let finished):
      ReadingResultView(finished: finished, onRestart: onRestart)
    }
  }
}

private struct InProgressView: View {
  let session: ReadingSessionUiState.InProgress
  let onAnswer: (String, String) -> Void
  let onSubmit: () -> Void
  @State private var isConfirmingSubmit = false

  private let twoPaneMinWidth: CGFloat = 840
  private let timeRunningOutSeconds: Int32 = 60

  var body: some View {
    GeometryReader { proxy in
      if proxy.size.width >= twoPaneMinWidth {
        HStack(alignment: .top, spacing: 0) {
          ScrollView { PassagesView(passages: session.test.passages).padding(24) }
          Divider()
          ScrollView { QuestionsView(session: session, onAnswer: onAnswer).padding(24) }
        }
      } else {
        ScrollView {
          VStack(alignment: .leading, spacing: 24) {
            PassagesView(passages: session.test.passages)
            Divider()
            QuestionsView(session: session, onAnswer: onAnswer)
          }
          .padding()
        }
      }
    }
    .navigationTitle(session.test.title)
    #if os(iOS)
    .navigationBarTitleDisplayMode(.inline)
    #endif
    .toolbar {
      ToolbarItem(placement: .primaryAction) {
        HStack(spacing: 12) {
          Text(session.remainingLabel)
            .monospacedDigit()
            .foregroundStyle(session.remainingSeconds <= timeRunningOutSeconds ? Color.red : Color.primary)
          Button("reading_session_submit") { isConfirmingSubmit = true }
            .buttonStyle(.borderedProminent)
        }
      }
    }
    .alert("reading_session_submit_title", isPresented: $isConfirmingSubmit) {
      Button("reading_session_continue", role: .cancel) {}
      Button("reading_session_submit", action: onSubmit)
    } message: {
      Text(submitMessage)
    }
  }

  private var submitMessage: String {
    let total = session.test.questionCount
    let unanswered = total - session.answeredCount
    if unanswered > 0 {
      return String(format: String(localized: "reading_session_submit_unanswered"), unanswered, total)
    }
    return String(format: String(localized: "reading_session_submit_all_answered"), total)
  }
}

private struct PassagesView: View {
  let passages: [Passage]

  var body: some View {
    VStack(alignment: .leading, spacing: 12) {
      ForEach(passages, id: \.id) { passage in
        Text(passage.title)
          .font(.title2.bold())
        ForEach(Array(passage.paragraphs.enumerated()), id: \.offset) { _, paragraph in
          paragraphText(paragraph)
            .font(.body)
            .fixedSize(horizontal: false, vertical: true)
        }
      }
    }
    .textSelection(.enabled)
  }

  private func paragraphText(_ paragraph: Paragraph) -> Text {
    guard let label = paragraph.label else { return Text(paragraph.text) }
    return Text(verbatim: label).bold() + Text(verbatim: "  ") + Text(paragraph.text)
  }
}

private struct QuestionsView: View {
  let session: ReadingSessionUiState.InProgress
  let onAnswer: (String, String) -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 20) {
      ForEach(session.test.passages.flatMap { $0.questionGroups }, id: \.id) { group in
        Text(group.instruction)
          .font(.subheadline.weight(.semibold))
          .foregroundStyle(Color.accentColor)
        ForEach(group.questions, id: \.id) { question in
          QuestionItemView(
            group: group,
            question: question,
            answer: session.answerFor(questionId: question.id),
            onAnswer: { onAnswer(question.id, $0) }
          )
        }
      }
    }
  }
}

private struct QuestionItemView: View {
  let group: QuestionGroup
  let question: Question
  let answer: String
  let onAnswer: (String) -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text(String(format: String(localized: "reading_session_question"), question.number, question.prompt))
        .fixedSize(horizontal: false, vertical: true)

      switch group.type {
      case .trueFalseNotGiven, .yesNoNotGiven:
        HStack(spacing: 8) {
          ForEach(group.type.fixedChoices, id: \.self) { choice in
            let isSelected = answer.caseInsensitiveCompare(choice) == .orderedSame
            Button(choice) { onAnswer(choice) }
              .buttonStyle(.bordered)
              .tint(isSelected ? Color.accentColor : Color.secondary)
              .fontWeight(isSelected ? .semibold : .regular)
          }
        }

      case .multipleChoice:
        VStack(alignment: .leading, spacing: 6) {
          ForEach(question.options, id: \.key) { option in
            Button {
              onAnswer(option.key)
            } label: {
              HStack(alignment: .top) {
                Image(systemName: answer == option.key ? "largecircle.fill.circle" : "circle")
                Text(String(format: String(localized: "reading_session_option"), option.key, option.text))
                  .multilineTextAlignment(.leading)
              }
            }
            .buttonStyle(.plain)
          }
        }

      case .sentenceCompletion:
        TextField(placeholder, text: Binding(get: { answer }, set: { onAnswer($0) }))
          .textFieldStyle(.roundedBorder)
          #if os(iOS)
        .textInputAutocapitalization(.never)
          #endif
          .autocorrectionDisabled()
      }
    }
  }

  private var placeholder: String {
    guard let maxWords = group.maxWords else { return String(localized: "reading_session_answer_placeholder") }
    return String(format: String(localized: "reading_session_max_words"), maxWords.int32Value)
  }
}

#Preview("Loading") {
  ReadingSessionContent(
    state: ReadingSessionUiState.Loading.shared,
    onRetry: {}, onAnswer: { _, _ in }, onSubmit: {}, onRestart: {}
  )
}

#Preview("Failed") {
  ReadingSessionContent(
    state: ReadingPreviewData.sessionFailed,
    onRetry: {}, onAnswer: { _, _ in }, onSubmit: {}, onRestart: {}
  )
}

#Preview("In progress") {
  NavigationStack {
    ReadingSessionContent(
      state: ReadingPreviewData.sessionInProgress,
      onRetry: {}, onAnswer: { _, _ in }, onSubmit: {}, onRestart: {}
    )
  }
}

#Preview("Finished") {
  NavigationStack {
    ReadingSessionContent(
      state: ReadingPreviewData.sessionFinished,
      onRetry: {}, onAnswer: { _, _ in }, onSubmit: {}, onRestart: {}
    )
  }
}
