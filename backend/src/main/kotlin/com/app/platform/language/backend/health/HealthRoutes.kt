package com.app.platform.language.backend.health

import com.app.platform.language.backend.config.AppEnv
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
  val status: String,
  val version: String,
  val env: String,
  val database: String,
)

fun Route.healthRoutes(
  version: String,
  env: AppEnv,
  databaseHealth: DatabaseHealth,
) {
  get("/health") {
    if (databaseHealth.isReachable()) {
      call.respond(HttpStatusCode.OK, HealthResponse(status = "ok", version = version, env = env.id, database = "up"))
    } else {
      call.respond(
        HttpStatusCode.ServiceUnavailable,
        HealthResponse(status = "degraded", version = version, env = env.id, database = "down"),
      )
    }
  }
}
