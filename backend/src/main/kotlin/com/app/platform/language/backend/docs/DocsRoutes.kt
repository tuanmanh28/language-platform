package com.app.platform.language.backend.docs

import com.app.platform.language.backend.config.AppEnv
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

const val OPENAPI_SPEC_RESOURCE = "openapi/documentation.yaml"

fun Route.docsRoutes(env: AppEnv) {
  if (env != AppEnv.PROD) swaggerUI(path = "docs", swaggerFile = OPENAPI_SPEC_RESOURCE)
}
