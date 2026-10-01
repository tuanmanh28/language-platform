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
        // iOS Simulator và app Mac gọi được backend chạy trên máy qua localhost.
        // Chạy trên iPhone thật: đổi thành IP LAN của máy chạy backend.
        return "http://localhost:8080"
        #else
        return "https://api.example.com"
        #endif
    }
}
