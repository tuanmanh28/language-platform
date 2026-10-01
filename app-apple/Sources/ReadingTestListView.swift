import Shared
import SwiftUI

struct ReadingTestListView: View {
  @StateObject private var holder = ViewModelHolder(ViewModels.shared.readingTestList())
  private var viewModel: ReadingTestListViewModel { holder.viewModel }

  var body: some View {
    NavigationStack {
      Observing(viewModel.state) { state in
        switch onEnum(of: state) {
        case .loading:
          ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)

        case .error(let error):
          ErrorStateView(message: error.message) { viewModel.refresh() }

        case .success(let success):
          List {
            if success.isOffline {
              Section {
                Label(
                  "Không kết nối được máy chủ — đang dùng đề đã lưu trên máy.",
                  systemImage: "wifi.slash"
                )
                .font(.callout)
                .foregroundStyle(.secondary)
              }
            }
            Section("Đề luyện") {
              ForEach(success.tests, id: \.id) { test in
                NavigationLink(value: test.id) {
                  TestRow(test: test)
                }
              }
            }
          }
        }
      }
      .navigationTitle("Luyện IELTS Reading")
      .navigationDestination(for: String.self) { testId in
        ReadingSessionView(testId: testId)
      }
      .toolbar {
        ToolbarItem(placement: .primaryAction) {
          Button {
            viewModel.refresh()
          } label: {
            Label("Tải lại", systemImage: "arrow.clockwise")
          }
        }
      }
    }
  }
}

private struct TestRow: View {
  let test: ReadingTestSummary

  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(test.title)
        .font(.headline)
      Text("\(moduleLabel(test.module)) · \(test.questionCount) câu · \(test.timeLimitMinutes) phút")
        .font(.subheadline)
        .foregroundStyle(.secondary)
    }
    .padding(.vertical, 4)
  }
}

func moduleLabel(_ module: IeltsModule) -> String {
  switch module {
  case .academic: return "Academic"
  case .generalTraining: return "General Training"
  }
}

struct ErrorStateView: View {
  let message: String
  let onRetry: () -> Void

  var body: some View {
    VStack(spacing: 12) {
      Image(systemName: "exclamationmark.triangle")
        .font(.largeTitle)
        .foregroundStyle(.orange)
      Text(message)
        .multilineTextAlignment(.center)
      Button("Thử lại", action: onRetry)
        .buttonStyle(.borderedProminent)
    }
    .padding()
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}
