package com.app.platform.language.backend.user

import com.app.platform.language.backend.auth.AuthIdentity
import com.app.platform.language.backend.database.AppDatabase
import com.app.platform.language.backend.database.PostgresTestDatabase
import com.app.platform.language.backend.fake.FakeAttemptStore
import com.app.platform.language.backend.fake.FakeDatabaseHealth
import com.app.platform.language.backend.fake.FakeTokenVerifier
import com.app.platform.language.backend.fake.FakeWritingSubmissionStore
import com.app.platform.language.backend.module
import com.app.platform.language.core.model.ContentJson
import com.app.platform.language.core.model.UserProfile
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DatabaseUserStoreTest {
  private val identity = AuthIdentity(uid = "firebase-uid-1", email = "learner@example.com", displayName = "Lan")
  private val store = DatabaseUserStore(database)

  @BeforeTest
  fun cleanDatabase() = runTest { PostgresTestDatabase.clean() }

  @Test
  fun validTokenCreatesUserRowOnce() =
    testApplication {
      application {
        module(
          FakeDatabaseHealth(),
          store,
          FakeAttemptStore(),
          FakeWritingSubmissionStore(),
          tokenVerifier = FakeTokenVerifier(mapOf("valid-token" to identity)),
        )
      }
      val client = createClient { install(ContentNegotiation) { json(ContentJson) } }

      val first = client.get("/api/v1/me") { bearerAuth("valid-token") }
      val second = client.get("/api/v1/me") { bearerAuth("valid-token") }

      assertEquals(HttpStatusCode.OK, first.status)
      assertEquals(first.body<UserProfile>(), second.body<UserProfile>())
      assertEquals(listOf(first.body<UserProfile>()), storedProfiles())
    }

  @Test
  fun upsertUpdatesChangedProfileAndKeepsId() =
    runTest {
      val created = store.upsert(identity)

      val updated = store.upsert(identity.copy(email = "new@example.com", displayName = null))

      assertEquals(User(created.id, "new@example.com", null), updated)
      assertEquals(listOf(updated.toProfile()), storedProfiles())
    }

  @Test
  fun differentFirebaseUsersGetDifferentRows() =
    runTest {
      val first = store.upsert(identity)
      val second = store.upsert(identity.copy(uid = "firebase-uid-2"))

      assertNotEquals(first.id, second.id)
      assertEquals(2, storedProfiles().size)
    }

  private suspend fun storedProfiles(): List<UserProfile> =
    database.tx {
      UsersTable.selectAll().map {
        UserProfile(it[UsersTable.id].toString(), it[UsersTable.email], it[UsersTable.displayName])
      }
    }

  companion object {
    private lateinit var database: AppDatabase

    @JvmStatic
    @BeforeAll
    fun connectDatabase() {
      database = PostgresTestDatabase.connect()
    }
  }
}
