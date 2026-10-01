package com.app.platform.language.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import java.util.UUID

private const val REQUEST_ID_MDC_KEY = "requestId"

private val requestIdPattern = Regex("[A-Za-z0-9._-]{1,128}")

fun Application.configureMonitoring() {
  install(CallId) {
    retrieveFromHeader(HttpHeaders.XRequestId)
    generate { UUID.randomUUID().toString() }
    verify { requestIdPattern.matches(it) }
    replyToHeader(HttpHeaders.XRequestId)
  }
  install(CallLogging) {
    callIdMdc(REQUEST_ID_MDC_KEY)
  }
  monitor.subscribe(ApplicationStopping) { log.info("Shutting down, draining in-flight requests") }
}
