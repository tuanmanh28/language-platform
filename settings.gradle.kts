pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Downloads JDK 17 for jvmToolchain(17) if only another JDK is installed (e.g. Android Studio's JBR 21).
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "language-platform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core:model")
include(":core:exam-engine")
include(":backend")

// -PbackendOnly builds the server without the Android SDK (e.g. inside the backend Docker image).
val backendOnly = providers.gradleProperty("backendOnly").map { it.isEmpty() || it.toBoolean() }.getOrElse(false)
if (!backendOnly) {
  include(":shared")
  include(":ui-compose")
  include(":app-android")
  include(":app-desktop")
}
