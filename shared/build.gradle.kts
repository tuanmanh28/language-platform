plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.skie)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.app.platform.language.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // Desktop (Windows) runs on the JVM
    jvm()

    // iOS + macOS: exports the "Shared" framework for the SwiftUI app (see app-apple/)
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            export(projects.core.model)
            export(projects.core.examEngine)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.examEngine)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.ktor.client.common)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            api(libs.koin.core)
            api(libs.koin.core.viewmodel)
            api(libs.androidx.lifecycle.viewmodel.kmp)
            api(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.swing)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("LanguagePlatformDatabase") {
            packageName.set("com.app.platform.language.shared.db")
        }
    }
}

skie {
    features {
        // Enables Observing(viewModel.state) { ... } in SwiftUI
        enableSwiftUIObservingPreview = true
    }
}
