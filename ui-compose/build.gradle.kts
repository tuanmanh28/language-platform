plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlinx.serialization)
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
      implementation(libs.androidx.lifecycle.viewmodel.navigation3)
      implementation(libs.androidx.navigation3.ui)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.compose.ui.test)
      implementation(compose.desktop.currentOs)
    }
  }
}

compose.resources {
  packageOfResClass = "com.app.platform.language.ui.resources"
}
