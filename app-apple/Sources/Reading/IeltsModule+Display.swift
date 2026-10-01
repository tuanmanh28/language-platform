import Shared

extension IeltsModule {
  var label: String {
    switch self {
    case .academic: return String(localized: "reading_common_module_academic")
    case .generalTraining: return String(localized: "reading_common_module_general_training")
    }
  }
}
