package com.app.platform.language.backend

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.health.healthRoutes
import com.app.platform.language.backend.plugins.configureCors
import com.app.platform.language.backend.plugins.configureMonitoring
import com.app.platform.language.backend.plugins.configureSerialization
import com.app.platform.language.backend.plugins.configureStatusPages
import com.app.platform.language.backend.reading.BundledContentStore
import com.app.platform.language.backend.reading.ContentStore
import com.app.platform.language.backend.reading.ReadingService
import com.app.platform.language.backend.reading.readingRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
  val config = AppConfig.fromEnvironment()
  embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
    module()
  }.start(wait = true)
}

fun Application.module(contentStore: ContentStore = BundledContentStore()) {
  configureSerialization()
  configureMonitoring()
  configureCors()
  configureStatusPages()

  val readingService = ReadingService(contentStore)
  routing {
    healthRoutes()
    readingRoutes(readingService)
  }
}
