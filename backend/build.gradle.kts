import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.shadowPlugin)
  application
}

version = "0.1.0"

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("com.app.platform.language.backend.ApplicationKt")
}

dependencies {
  // Same scoring code as the apps, so server and on-device scores always match.
  implementation(projects.core.model)
  implementation(projects.core.examEngine)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.cors)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.server.call.logging)
  implementation(libs.ktor.server.call.id)
  implementation(libs.ktor.server.status.pages)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.logback.classic)
  implementation(libs.logstash.logback.encoder)
  implementation(libs.exposed.core)
  implementation(libs.exposed.jdbc)
  implementation(libs.flyway.core)
  implementation(libs.hikari)
  runtimeOnly(libs.flyway.database.postgresql)
  runtimeOnly(libs.postgresql)

  testImplementation(kotlin("test"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.ktor.server.test.host)
  testImplementation(libs.ktor.client.content.negotiation)
  testImplementation(libs.testcontainers.postgresql)
}

val backendVersion = version.toString()

val writeBuildInfo by tasks.registering(WriteProperties::class) {
  destinationFile = layout.buildDirectory.file("generated/build-info/build-info.properties")
  property("version", backendVersion)
}

tasks.processResources {
  from(writeBuildInfo)
}

tasks.test {
  useJUnitPlatform()
  systemProperty("backend.expectedVersion", backendVersion)
}

tasks.named<ShadowJar>("shadowJar") {
  archiveFileName.set("backend-all.jar")
  mergeServiceFiles()
}
