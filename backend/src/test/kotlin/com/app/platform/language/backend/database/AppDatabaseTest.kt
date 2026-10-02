package com.app.platform.language.backend.database

import com.app.platform.language.backend.config.DatabaseConfig
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class AppDatabaseTest {
  private data class StoredAttempt(
    val firebaseUid: String,
    val testTitle: String,
    val correctCount: Int,
    val band: BigDecimal,
    val firstAnswer: String,
  )

  @BeforeTest
  fun cleanTables() =
    runTest {
      database.tx { exec("TRUNCATE writing_submissions, reading_attempts, users, reading_tests") }
    }

  @Test
  fun attemptRoundTripsThroughMigratedSchema() =
    runTest {
      val userId = UUID.randomUUID()

      database.tx {
        exec(
          "INSERT INTO reading_tests (id, module, title, time_limit_minutes, content) " +
            "VALUES ('cambridge-18-test-1', 'academic', 'Cambridge 18 Test 1', 60, '{\"passages\": []}')",
        )
        exec("INSERT INTO users (id, firebase_uid, email) VALUES ('$userId', 'firebase-1', 'learner@example.com')")
        exec(
          "INSERT INTO reading_attempts (user_id, client_id, test_id, correct_count, total_questions, band, answers) " +
            "VALUES ('$userId', 'client-1', 'cambridge-18-test-1', 30, 40, 7.0, '{\"q1\": \"TRUE\"}')",
        )
      }
      val stored =
        database.tx {
          exec(
            "SELECT u.firebase_uid, t.title, a.correct_count, a.band, a.answers ->> 'q1' AS first_answer " +
              "FROM reading_attempts a JOIN users u ON u.id = a.user_id JOIN reading_tests t ON t.id = a.test_id",
          ) { rows ->
            rows.next()
            StoredAttempt(
              firebaseUid = rows.getString("firebase_uid"),
              testTitle = rows.getString("title"),
              correctCount = rows.getInt("correct_count"),
              band = rows.getBigDecimal("band"),
              firstAnswer = rows.getString("first_answer"),
            )
          }
        }

      assertEquals(StoredAttempt("firebase-1", "Cambridge 18 Test 1", 30, BigDecimal("7.0"), "TRUE"), stored)
    }

  @Test
  fun databaseIsReachableWhileRunning() =
    runTest {
      assertTrue(database.isReachable())
    }

  @Test
  fun closedDatabaseIsNotReachable() =
    runTest {
      val closed = AppDatabase.connect(postgres.toDatabaseConfig()).getOrElse { fail(it.message) }
      closed.close()

      assertFalse(closed.isReachable())
    }

  @Test
  fun reconnectingToMigratedDatabaseSucceeds() {
    AppDatabase.connect(postgres.toDatabaseConfig()).getOrElse { fail(it.message) }.close()
  }

  @Test
  fun conflictingSchemaFailsMigration() {
    val conflictingUrl = postgres.jdbcUrl.replace("/${postgres.databaseName}", "/conflicting")
    DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use {
      it.createStatement().execute("CREATE DATABASE conflicting")
    }
    DriverManager.getConnection(conflictingUrl, postgres.username, postgres.password).use {
      it.createStatement().execute("CREATE TABLE users (id INT)")
    }

    val result = AppDatabase.connect(DatabaseConfig(conflictingUrl, postgres.username, postgres.password))

    assertIs<DatabaseError.MigrationFailed>(result.getError())
  }

  companion object {
    private val postgres = PostgreSQLContainer("postgres:17")
    private lateinit var database: AppDatabase

    private fun PostgreSQLContainer.toDatabaseConfig() = DatabaseConfig(jdbcUrl, username, password)

    @JvmStatic
    @BeforeAll
    fun startDatabase() {
      assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available: skipping PostgreSQL tests")
      postgres.start()
      database = AppDatabase.connect(postgres.toDatabaseConfig()).getOrElse { fail(it.message) }
    }

    @JvmStatic
    @AfterAll
    fun stopDatabase() {
      if (::database.isInitialized) database.close()
      postgres.stop()
    }
  }
}
