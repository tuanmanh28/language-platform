plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  jvmToolchain(17)

  android {
    namespace = "com.app.platform.language.core.exam"
    compileSdk =
      libs.versions.compileSdk
        .get()
        .toInt()
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
    withHostTest {}
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()

  sourceSets {
    commonMain.dependencies {
      api(projects.core.model)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}
