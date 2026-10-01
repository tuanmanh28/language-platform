pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Tự tải JDK 17 cho jvmToolchain(17) nếu máy chỉ có JDK khác (vd. JBR 21 của Android Studio).
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

// Core thuần Kotlin, dùng chung cho app VÀ backend
include(":core:model")
include(":core:exam-engine")

// Tầng dữ liệu + ViewModel dùng chung cho mọi app native (KMP)
include(":shared")

// UI Compose dùng chung Android + Desktop (Windows)
include(":ui-compose")

// Entry point từng nền tảng
include(":app-android")
include(":app-desktop")

// Server
include(":backend")
