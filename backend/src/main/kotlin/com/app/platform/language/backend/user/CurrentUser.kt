package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.plugins.FIREBASE_AUTH
import com.github.michaelbull.result.mapBoth
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey

class CurrentUserConfig {
  lateinit var service: UserService
}

private val currentUserKey = AttributeKey<User>("CurrentUser")

val CurrentUser =
  createRouteScopedPlugin("CurrentUser", ::CurrentUserConfig) {
    val service = pluginConfig.service
    on(AuthenticationChecked) { call ->
      val identity = call.principal<AuthIdentity>() ?: return@on
      service.syncUser(identity).mapBoth(
        success = { user -> call.attributes.put(currentUserKey, user) },
        failure = { error -> call.respondError(error) },
      )
    }
  }

fun Route.authenticatedUser(
  userService: UserService,
  build: Route.() -> Unit,
): Route =
  authenticate(FIREBASE_AUTH) {
    install(CurrentUser) { service = userService }
    build()
  }

fun ApplicationCall.currentUser(): User = attributes[currentUserKey]

private suspend fun ApplicationCall.respondError(error: UserError) {
  if (error is UserError.Unexpected) application.log.error("User sync failed", error.cause)
  val (status, body) = error.toHttp()
  respond(status, body)
}
