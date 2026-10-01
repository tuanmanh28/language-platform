import Shared
import SwiftUI

struct ReadingTestListView: View {
  @State private var owner: ViewModelOwner<ReadingTestListViewModel>?
  @State private var state: ReadingTestListUiState = ReadingTestListUiState.Loading.shared

  var body: some View {
    NavigationStack {
      ReadingTestListContent(state: state, onRefresh: { owner?.viewModel.refresh() })
        .navigationDestination(for: String.self) { testId in
          ReadingSessionView(testId: testId)
        }
    }
    .task {
      let owner = ViewModels.shared.readingTestList()
      self.owner = owner
      await withTaskCancellationHandler {
        for await value in owner.viewModel.state { state = value }
      } onCancel: {
        owner.clear()
      }
    }
  }
}

struct ReadingTestListContent: View {
  let state: ReadingTestListUiState
  let onRefresh: () -> Void

  var body: some View {
    Group {
      switch onEnum(of: state) {
      case .loading:
        ProgressView()
          .frame(maxWidth: .infinity, maxHeight: .infinity)

      case .ready(let ready):
        ReadyList(ready: ready)
      }
    }
    .navigationTitle("reading_list_title")
    .toolbar {
      ToolbarItem(placement: .primaryAction) {
        Button(action: onRefresh) {
          Label("reading_list_refresh", systemImage: "arrow.clockwise")
        }
      }
    }
  }
}

private struct ReadyList: View {
  let ready: ReadingTestListUiState.Ready

  var body: some View {
    List {
      if ready.isOffline {
        Section {
          Label("reading_list_offline_banner", systemImage: "wifi.slash")
            .font(.callout)
            .foregroundStyle(.secondary)
        }
      }
      Section("reading_list_section_title") {
        ForEach(ready.tests, id: \.id) { test in
          NavigationLink(value: test.id) {
            TestRow(test: test)
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
      Text(
        String(
          format: String(localized: "reading_list_test_details"),
          test.module.label,
          test.questionCount,
          test.timeLimitMinutes
        )
      )
      .font(.subheadline)
      .foregroundStyle(.secondary)
    }
    .padding(.vertical, 4)
  }
}

#Preview("Loading") {
  NavigationStack {
    ReadingTestListContent(state: ReadingTestListUiState.Loading.shared, onRefresh: {})
  }
}

#Preview("Ready") {
  NavigationStack {
    ReadingTestListContent(state: ReadingPreviewData.listReady(isOffline: false), onRefresh: {})
  }
}

#Preview("Empty") {
  NavigationStack {
    ReadingTestListContent(state: ReadingPreviewData.listEmpty, onRefresh: {})
  }
}

#Preview("Offline") {
  NavigationStack {
    ReadingTestListContent(state: ReadingPreviewData.listReady(isOffline: true), onRefresh: {})
  }
}
