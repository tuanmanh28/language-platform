import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.uiCompose)
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.app.platform.language.desktop.MainKt"

        nativeDistributions {
            // Windows: .msi/.exe. (.dmg is only for trying it on a Mac — the real macOS app is SwiftUI.)
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "LanguagePlatform"
            packageVersion = "0.1.0"
            vendor = "Language Platform"

            windows {
                menuGroup = "Language Platform"
                // Keep fixed so new installers upgrade old ones. Regenerate with uuidgen if needed.
                upgradeUuid = "e27b1e61-c0aa-4185-9f5b-b01259ee9ab1"
            }
        }
    }
}
