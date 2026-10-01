package com.app.platform.language.backend.config

import java.util.Properties

object BuildInfo {
  val version: String = loadVersion()

  private fun loadVersion(): String {
    val stream =
      checkNotNull(BuildInfo::class.java.getResourceAsStream("/build-info.properties")) {
        "build-info.properties is missing from the classpath"
      }
    val properties = stream.use { Properties().apply { load(it) } }
    return checkNotNull(properties.getProperty("version")) { "build-info.properties has no version" }
  }
}
