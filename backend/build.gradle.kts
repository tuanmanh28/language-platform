import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadowPlugin)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.app.platform.language.backend.ApplicationKt")
}

dependencies {
    // Cùng model + logic chấm với app → kết quả chấm ở server và trên máy luôn khớp
    implementation(projects.core.model)
    implementation(projects.core.examEngine)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}

tasks.test {
    useJUnitPlatform()
}

// Fat jar dùng cho Docker: backend/build/libs/backend-all.jar
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("backend-all.jar")
    mergeServiceFiles()
}
