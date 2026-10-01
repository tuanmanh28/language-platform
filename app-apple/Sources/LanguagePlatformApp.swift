import Shared
import SwiftUI

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
    // Simulator and Mac reach a local backend via localhost; a real iPhone needs this machine's LAN IP.
    return "http://localhost:8080"
    #else
    return "https://api.example.com"
    #endif
  }
}
