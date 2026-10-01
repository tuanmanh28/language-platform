package com.app.platform.language.backend.health

import com.app.platform.language.backend.config.AppEnv
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
  val status: String,
  val version: String,
  val env: String,
)

fun Route.healthRoutes(
  version: String,
  env: AppEnv,
) {
  val health = HealthResponse(status = "ok", version = version, env = env.id)
  get("/health") {
    call.respond(health)
  }
}
