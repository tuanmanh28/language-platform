package com.app.platform.language.backend.docs

import com.app.platform.language.backend.config.AppConfig
import com.app.platform.language.backend.config.AppEnv
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeUserStore
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.plugin
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.PathSegmentParameterRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.swagger.v3.parser.OpenAPIV3Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocsRoutesTest {
  private fun ApplicationTestBuilder.startIn(env: AppEnv) =
    application {
      module(
        FakeDatabaseHealth(),
        FakeUserStore(),
        FakeAttemptStore(),
        FakeWritingSubmissionStore(),
        AppConfig.local.copy(env = env),
      )
    }

  @Test
  fun swaggerUiIsServedOutsideProd() =
    testApplication {
      startIn(AppEnv.STAGING)

      val response = client.get("/docs")

      assertEquals(HttpStatusCode.OK, response.status)
      assertTrue("swagger-ui" in response.bodyAsText())
    }

  @Test
  fun docsAreHiddenInProd() =
    testApplication {
      startIn(AppEnv.PROD)

      assertEquals(HttpStatusCode.NotFound, client.get("/docs").status)
      assertEquals(HttpStatusCode.NotFound, client.get("/docs/documentation.yaml").status)
    }

  @Test
  fun servedSpecIsValidOpenApi() =
    testApplication {
      startIn(AppEnv.LOCAL)

      val result = OpenAPIV3Parser().readContents(client.get("/docs/documentation.yaml").bodyAsText())

      assertEquals(emptyList(), result.messages)
    }

  @Test
  fun everyApiRouteIsDocumented() =
    testApplication {
      var routingRoot: RoutingRoot? = null
      application {
        module(FakeDatabaseHealth(), FakeUserStore(), FakeAttemptStore(), FakeWritingSubmissionStore())
        routingRoot = plugin(RoutingRoot)
      }
      val spec = OpenAPIV3Parser().readContents(client.get("/docs/documentation.yaml").bodyAsText()).openAPI

      val registered = checkNotNull(routingRoot).getAllRoutes().mapNotNull { it.endpoint() }.toApiEndpoints()
      val documented =
        spec.paths
          .flatMap { (path, item) -> item.readOperationsMap().keys.map { method -> "$method $path" } }
          .toApiEndpoints()

      assertTrue(registered.isNotEmpty())
      assertEquals(registered, documented)
    }

  private fun List<String>.toApiEndpoints(): Set<String> = filter { " /api/v1/" in it }.toSet()

  private fun RoutingNode.endpoint(): String? {
    val method = (selector as? HttpMethodRouteSelector)?.method ?: return null
    val segments =
      generateSequence(parent) { it.parent }.toList().asReversed().mapNotNull { node ->
        when (val segment = node.selector) {
          is PathSegmentConstantRouteSelector -> segment.value
          is PathSegmentParameterRouteSelector -> "{${segment.name}}"
          else -> null
        }
      }
    return "${method.value} ${segments.joinToString("/", prefix = "/")}"
  }
}
