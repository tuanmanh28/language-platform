import SwiftUI

/// Giữ một ViewModel Kotlin theo vòng đời của view SwiftUI (dùng với @StateObject).
/// State thật nằm trong StateFlow của ViewModel và được theo dõi bằng `Observing` của SKIE,
/// nên holder không cần phát objectWillChange.
final class ViewModelHolder<VM: AnyObject>: ObservableObject {
    let viewModel: VM
    private let onRelease: ((VM) -> Void)?

    init(_ viewModel: @autoclosure () -> VM, onRelease: ((VM) -> Void)? = nil) {
        self.viewModel = viewModel()
        self.onRelease = onRelease
    }

    deinit {
        onRelease?(viewModel)
    }
}
