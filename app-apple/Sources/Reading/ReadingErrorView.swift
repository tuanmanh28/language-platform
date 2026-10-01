import SwiftUI

struct ReadingErrorView: View {
  let message: String
  let onRetry: () -> Void

  var body: some View {
    VStack(spacing: 12) {
      Image(systemName: "exclamationmark.triangle")
        .font(.largeTitle)
        .foregroundStyle(.orange)

      Text(message)
        .multilineTextAlignment(.center)

      Button("common_retry", action: onRetry)
        .buttonStyle(.borderedProminent)
    }
    .padding()
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}

#Preview {
  ReadingErrorView(message: String(localized: "reading_common_error_offline"), onRetry: {})
}
