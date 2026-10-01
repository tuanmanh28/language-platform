package com.app.platform.language.backend.database

import com.app.platform.language.backend.config.DatabaseConfig
import com.github.michaelbull.result.getOrElse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.fail

object PostgresTestDatabase {
  private val postgres = PostgreSQLContainer("postgres:17")

  // Shared by every test class that needs it; Testcontainers removes the container when the JVM exits.
  private val database: AppDatabase by lazy {
    postgres.start()
    AppDatabase.connect(DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)).getOrElse {
      fail(it.message)
    }
  }

  fun connect(): AppDatabase {
    assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available: skipping PostgreSQL tests")
    return database
  }

  suspend fun clean() {
    database.tx { exec("TRUNCATE reading_attempts, users, reading_tests, listening_tests") }
  }
}
