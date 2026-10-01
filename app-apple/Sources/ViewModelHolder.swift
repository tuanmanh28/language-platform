import SwiftUI

/// Ties a Kotlin ViewModel to the lifetime of a SwiftUI view (use with @StateObject).
/// The real state lives in the ViewModel's StateFlow and is observed with SKIE's `Observing`,
/// so the holder never needs to publish objectWillChange.
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
