import Shared

enum ReadingPreviewData {
  private static let test = BundledReadingTests.shared.all.first!
  private static let firstQuestion = test.allQuestions().first!
  private static let answers = [firstQuestion.id: firstQuestion.acceptedAnswers.first!]

  static func listReady(isOffline: Bool) -> ReadingTestListUiState {
    ReadingTestListUiState.Ready(tests: BundledReadingTests.shared.all.map { $0.toSummary() }, isOffline: isOffline)
  }

  static let listEmpty: ReadingTestListUiState = ReadingTestListUiState.Ready(tests: [], isOffline: false)

  static let sessionFailed: ReadingSessionUiState = ReadingSessionUiState.Failed(error: ReadingErrorOffline.shared)

  static let sessionInProgress: ReadingSessionUiState =
    ReadingSessionUiState.InProgress(test: test, answers: answers, remainingSeconds: 45)

  static let sessionFinished = ReadingSessionUiState.Finished(
    test: test,
    result: ReadingScorer.shared.score(test: test, answers: answers),
    isTimeExpired: true
  )
}
