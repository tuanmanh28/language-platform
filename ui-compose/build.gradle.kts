plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

// Compose UI shared by Android (app-android) and Desktop/Windows (app-desktop).
// iOS/macOS do NOT use this module — Apple platforms use native SwiftUI (app-apple).
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
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

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
