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
      implementation(libs.roborazzi.compose.desktop)
      implementation(libs.composable.preview.scanner.android)
    }
  }
}

compose.resources {
  packageOfResClass = "com.app.platform.language.ui.resources"
}

val screenshotTest = "*.PreviewScreenshotTest"

tasks.named<Test>("jvmTest") {
  filter.excludeTestsMatching(screenshotTest)
}

tasks.register<Test>("screenshots") {
  description = "Renders every @Preview to build/screenshots."
  group = "verification"
  val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
  testClassesDirs = jvmTestCompilation.output.classesDirs
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  filter.includeTestsMatching(screenshotTest)
  systemProperty("roborazzi.test.record", "true")
  val screenshotsDir = layout.buildDirectory.dir("screenshots")
  outputs.dir(screenshotsDir)
  // Removed previews must not leave stale images behind.
  doFirst { screenshotsDir.get().asFile.deleteRecursively() }
}
