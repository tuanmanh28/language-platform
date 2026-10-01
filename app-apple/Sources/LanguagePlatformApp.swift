import SwiftUI
import Shared

@main
struct LanguagePlatformApp: App {
    init() {
        SharedSdk.shared.start(baseUrl: ApiEnvironment.baseUrl)
    }

    var body: some Scene {
        WindowGroup {
            ReadingTestListView()
        }
        #if os(macOS)
        .defaultSize(width: 1200, height: 800)
        #endif
    }
}

enum ApiEnvironment {
    static var baseUrl: String {
        #if DEBUG
        // The iOS Simulator and the Mac app reach a backend running on this machine via localhost.
        // On a real iPhone, use the LAN IP of the machine running the backend.
        return "http://localhost:8080"
        #else
        return "https://api.example.com"
        #endif
    }
}
