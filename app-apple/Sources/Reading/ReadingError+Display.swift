import Shared

extension ReadingError {
  var userMessage: String {
    switch onEnum(of: self) {
    case .notFound: return String(localized: "reading_common_error_not_found")
    case .offline: return String(localized: "reading_common_error_offline")
    case .unexpected: return String(localized: "reading_common_error_unexpected")
    }
  }
}
