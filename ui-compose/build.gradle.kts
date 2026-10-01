plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
}

// Apple platforms use SwiftUI (app-apple), so this module only targets Android and Desktop.
kotlin {
  jvmToolchain(17)

  android {
    namespace = "com.app.platform.language.ui"
    compileSdk =
      libs.versions.compileSdk
        .get()
        .toInt()
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
    androidResources {
      enable = true
    }
  }
  jvm()

  sourceSets {
    commonMain.dependencies {
      api(projects.shared)

      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.ui.tooling.preview)

      implementation(libs.androidx.lifecycle.viewmodel.compose.kmp)
      implementation(libs.androidx.lifecycle.runtime.compose.kmp)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
    }
  }
}

compose.resources {
  packageOfResClass = "com.app.platform.language.ui.resources"
}
