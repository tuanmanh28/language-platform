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

// Pure Kotlin core, shared by the apps AND the backend
include(":core:model")
include(":core:exam-engine")

// Data layer + ViewModels shared by every native app (KMP)
include(":shared")

// Compose UI shared by Android + Desktop (Windows)
include(":ui-compose")

// Per-platform entry points
include(":app-android")
include(":app-desktop")

// Server
include(":backend")
