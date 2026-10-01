import Shared
import SwiftUI

struct ReadingResultView: View {
    let finished: ReadingSessionUiState.Finished
    let onRestart: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let result = finished.result

        List {
            Section {
                VStack(spacing: 8) {
                    Text("Band ước tính")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(String(format: "%.1f", result.band))
                        .font(.system(size: 56, weight: .bold, design: .rounded))
                    Text("Đúng \(result.correctCount)/\(result.totalQuestions) câu")
                        .font(.headline)
                    if finished.timeExpired {
                        Text("Hết giờ — bài đã được nộp tự động.")
                            .foregroundStyle(.red)
                    }
                    HStack(spacing: 12) {
                        Button("Về danh sách") { dismiss() }
                            .buttonStyle(.bordered)
                        Button("Làm lại", action: onRestart)
                            .buttonStyle(.borderedProminent)
                    }
                    .padding(.top, 8)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }

            Section("Chi tiết") {
                ForEach(result.questionResults, id: \.questionId) { item in
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: item.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundStyle(item.isCorrect ? Color.green : Color.red)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Câu \(item.number)").font(.subheadline.bold())
                            Text("Bạn trả lời: \(item.userAnswer ?? "—")").font(.callout)
                            if !item.isCorrect {
                                Text("Đáp án: \(item.acceptedAnswers.joined(separator: " / "))")
                                    .font(.callout)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Kết quả")
    }
}
