import Shared
import SwiftUI

struct ReadingSessionView: View {
  let testId: String
  // @StateObject creates the holder only ONCE per appearance of the screen (autoclosure),
  // so re-creating the struct does not spawn extra ViewModels.
  @StateObject private var holder: ViewModelHolder<ReadingSessionViewModel>

  init(testId: String) {
    self.testId = testId
    _holder = StateObject(
      wrappedValue: ViewModelHolder(
        ViewModels.shared.readingSession(testId: testId),
        onRelease: { $0.stop() }
      ))
  }

  private var viewModel: ReadingSessionViewModel { holder.viewModel }

  var body: some View {
    Observing(viewModel.state) { state in
      switch onEnum(of: state) {
      case .loading:
        ProgressView()
          .frame(maxWidth: .infinity, maxHeight: .infinity)

      case .error(let error):
        ErrorStateView(message: error.message) { viewModel.retry() }

      case .inProgress(let session):
        InProgressView(session: session, viewModel: viewModel)

      case .finished(let finished):
        ReadingResultView(
          finished: finished,
          onRestart: { viewModel.restart() }
        )
      }
    }
    .onAppear { viewModel.start() }
    .onDisappear { viewModel.stop() }
  }
}

private struct InProgressView: View {
  let session: ReadingSessionUiState.InProgress
  let viewModel: ReadingSessionViewModel
  @State private var confirmSubmit = false

  /// iPad landscape and Mac: passage and questions side by side.
  private let twoPaneMinWidth: CGFloat = 840

  var body: some View {
    GeometryReader { proxy in
      if proxy.size.width >= twoPaneMinWidth {
        HStack(alignment: .top, spacing: 0) {
          ScrollView { PassagesView(passages: session.test.passages).padding(24) }
          Divider()
          ScrollView { QuestionsView(session: session, viewModel: viewModel).padding(24) }
        }
      } else {
        ScrollView {
          VStack(alignment: .leading, spacing: 24) {
            PassagesView(passages: session.test.passages)
            Divider()
            QuestionsView(session: session, viewModel: viewModel)
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
            .foregroundStyle(session.remainingSeconds <= 60 ? Color.red : Color.primary)
          Button("Nộp bài") { confirmSubmit = true }
            .buttonStyle(.borderedProminent)
        }
      }
    }
    .alert("Nộp bài?", isPresented: $confirmSubmit) {
      Button("Làm tiếp", role: .cancel) {}
      Button("Nộp bài") { viewModel.submit() }
    } message: {
      let total = session.test.questionCount
      let unanswered = total - session.answeredCount
      Text(
        unanswered > 0
          ? "Bạn còn \(unanswered)/\(total) câu chưa trả lời."
          : "Bạn đã trả lời đủ \(total) câu.")
    }
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
    if let label = paragraph.label {
      return Text("\(label)  ").bold() + Text(paragraph.text)
    }
    return Text(paragraph.text)
  }
}

private struct QuestionsView: View {
  let session: ReadingSessionUiState.InProgress
  let viewModel: ReadingSessionViewModel

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
            onAnswer: { viewModel.answer(questionId: question.id, value: $0) }
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
      Text("\(question.number). \(question.prompt)")
        .fixedSize(horizontal: false, vertical: true)

      switch group.type {
      case .trueFalseNotGiven, .yesNoNotGiven:
        HStack(spacing: 8) {
          ForEach(fixedChoices(for: group.type), id: \.self) { choice in
            let selected = answer.caseInsensitiveCompare(choice) == .orderedSame
            Button(choice) { onAnswer(choice) }
              .buttonStyle(.bordered)
              .tint(selected ? Color.accentColor : Color.secondary)
              .fontWeight(selected ? .semibold : .regular)
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
                Text("\(option.key). \(option.text)")
                  .multilineTextAlignment(.leading)
              }
            }
            .buttonStyle(.plain)
          }
        }

      case .sentenceCompletion:
        TextField(
          group.maxWords.map { "Tối đa \($0.intValue) từ" } ?? "Câu trả lời",
          text: Binding(get: { answer }, set: { onAnswer($0) })
        )
        .textFieldStyle(.roundedBorder)
        #if os(iOS)
        .textInputAutocapitalization(.never)
        #endif
        .autocorrectionDisabled()
      }
    }
  }

  /// Mirrors QuestionType.fixedChoices in Kotlin; duplicated in Swift to avoid depending on how SKIE bridges enum members.
  private func fixedChoices(for type: QuestionType) -> [String] {
    switch type {
    case .trueFalseNotGiven: return ["TRUE", "FALSE", "NOT GIVEN"]
    case .yesNoNotGiven: return ["YES", "NO", "NOT GIVEN"]
    case .multipleChoice, .sentenceCompletion: return []
    }
  }
}
